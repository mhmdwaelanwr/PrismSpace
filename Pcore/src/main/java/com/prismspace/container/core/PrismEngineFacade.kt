package com.prismspace.container.core

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.prismspace.container.PrismSpaceCore
import com.prismspace.container.core.privacy.DeviceProfileManager
import com.prismspace.container.entity.location.PLocation
import com.prismspace.container.entity.pm.InstallResult
import com.prismspace.container.fake.frameworks.PLocationManager
import com.prismspace.container.utils.Slog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking

object PrismEngineFacade {
    private const val TAG = "PrismEngineFacade"
    private const val DEFAULT_USER_ID = 0
    private const val INSTALL_TIMEOUT_MS = 45_000L
    private const val LAUNCH_TIMEOUT_MS = 25_000L
    private const val SERVICE_READY_TIMEOUT_MS = 12_000L
    private const val POLICY_READY_TIMEOUT_MS = 2_500L
    private const val SERVICE_READY_STEP_MS = 200L

    private val initLock = Any()

    @Volatile
    private var initialized = false

    private val virtualAppsListeners = java.util.concurrent.CopyOnWriteArraySet<() -> Unit>()
    private val pendingVirtualPackages = java.util.concurrent.ConcurrentHashMap<Int, MutableSet<String>>()

    suspend fun initEngine(context: Context): Boolean = withContext(Dispatchers.IO) {
        val safeContext = context.applicationContext ?: context
        try {
            if (!initialized) {
                synchronized(initLock) {
                    if (!initialized) {
                        val core = PrismSpaceCore.get()
                        invokeCoreInit(core, safeContext)
                        core.ensureBlackProcessInitialized()
                        initialized = true
                    }
                }
            }
            true
        } catch (t: Throwable) {
            Slog.e(TAG, "Engine initialization failed", t)
            false
        }
    }

    suspend fun getInstalledApps(): List<ApplicationInfo> = withContext(Dispatchers.IO) {
        try {
            PrismSpaceCore.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (t: Throwable) {
            Slog.e(TAG, "Failed to query installed host apps", t)
            emptyList()
        }
    }

    fun addVirtualAppsChangedListener(listener: () -> Unit) {
        virtualAppsListeners.add(listener)
    }

    fun removeVirtualAppsChangedListener(listener: () -> Unit) {
        virtualAppsListeners.remove(listener)
    }

    private fun notifyVirtualAppsChanged() {
        for (listener in virtualAppsListeners) {
            runCatching { listener.invoke() }
                .onFailure { Slog.w(TAG, "virtualApps listener failed: ${it.message}") }
        }
    }

    private fun markPendingPackage(userId: Int, packageName: String) {
        val set = pendingVirtualPackages.getOrPut(userId) {
            java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())
        }
        if (set.add(packageName)) notifyVirtualAppsChanged()
    }

    private fun clearPendingPackage(userId: Int, packageName: String) {
        val set = pendingVirtualPackages[userId] ?: return
        val changed = set.remove(packageName)
        if (set.isEmpty()) {
            pendingVirtualPackages.remove(userId)
        }
        if (changed) notifyVirtualAppsChanged()
    }

    private fun ensureCoreServicesReady(): Boolean {
        val core = PrismSpaceCore.get()
        val ready = core.ensureCorePackageServicesReady(SERVICE_READY_TIMEOUT_MS)
        if (!ready) {
            Slog.w(TAG, "Core services still warming up after timeout; continuing to real core call")
        }
        return ready
    }

    private fun ensurePolicyServicesReady(): Boolean {
        val appContext = PrismSpaceCore.getContext() ?: return false
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < POLICY_READY_TIMEOUT_MS) {
            val ok = runCatching {
                ClonePolicyClient.get().bind(appContext)
                true
            }.getOrDefault(false)
            if (ok) {
                return true
            }
            Thread.sleep(SERVICE_READY_STEP_MS)
        }
        return false
    }

    private fun invokeCoreInit(core: PrismSpaceCore, context: Context) {
        try {
            core.javaClass.getMethod("init", Context::class.java).invoke(core, context)
        } catch (_: NoSuchMethodException) {
            core.doCreate()
        }
    }

    private fun preWarmPackageInternal(core: PrismSpaceCore, packageName: String, userId: Int) {
        runCatching {
            PrismSpaceCore.getBPackageManager().getLaunchIntentForPackage(packageName, userId)
            core.getInstalledPackages(PackageManager.GET_META_DATA, userId)
            core.getInstalledApplications(PackageManager.GET_META_DATA, userId)
        }
        val hooks = listOf("preWarmPackage", "optimizePackage", "dexOptPackage")
        for (h in hooks) {
            runCatching {
                val intType = Int::class.javaPrimitiveType ?: Int::class.java
                core.javaClass.getMethod(h, String::class.java, intType).invoke(core, packageName, userId)
            }
        }
    }

    suspend fun getVirtualizedApps(userId: Int = DEFAULT_USER_ID): List<ApplicationInfo> = withContext(Dispatchers.IO) {
        try {
            ensureCoreServicesReady()
            val installed = PrismSpaceCore.get().getInstalledApplications(PackageManager.GET_META_DATA, userId).toMutableList()
            val installedPkgs = installed.map { it.packageName }.toHashSet()
            val pending = pendingVirtualPackages[userId] ?: emptySet()
            if (pending.isNotEmpty()) {
                val hostPm = PrismSpaceCore.getPackageManager()
                for (pkg in pending) {
                    if (installedPkgs.contains(pkg)) continue
                    runCatching { hostPm.getApplicationInfo(pkg, PackageManager.GET_META_DATA) }
                        .onSuccess { installed.add(it) }
                }
            }
            installed
        } catch (t: Throwable) {
            Slog.e(TAG, "Failed to query virtualized apps for userId=$userId", t)
            emptyList()
        }
    }

    /**
     * H6: Integrated App Installation Flow (Hybrid).
     * Synchronously waits for installation but triggers new policy engine.
     */
    suspend fun installApp(packageName: String, userId: Int = DEFAULT_USER_ID): InstallResult = withContext(Dispatchers.IO) {
        val normalizedPackage = packageName.trim()
        if (normalizedPackage.isEmpty()) return@withContext InstallResult().installError("Package name is empty")

        markPendingPackage(userId, normalizedPackage)

        return@withContext try {
            withTimeout(INSTALL_TIMEOUT_MS) {
                val coreReady = ensureCoreServicesReady()
                val core = PrismSpaceCore.get()

                // 1) Core install path must not depend on policy readiness.
                val result = core.installPackageAsUser(normalizedPackage, userId)
                if (!result.success) {
                    if (!coreReady && result.msg.isNullOrBlank()) {
                        result.msg = "core install failed after readiness warm-up timeout"
                    }
                    return@withTimeout result
                }

                // 2) Prepare workspace/runtime redirects and warm-up.
                preWarmPackageInternal(core, normalizedPackage, userId)

                // 3) Visible app state should be refreshed as soon as install succeeds.
                notifyVirtualAppsChanged()

                // 4) Build snapshot and best-effort publish policy.
                var policyState = "OK"
                var policyError: String? = null
                runCatching {
                    val gen = System.currentTimeMillis()
                    val builder = ClonePolicyBuilder(gen)
                    builder.setCloneId(userId)
                    builder.setPackageName(normalizedPackage)

                    for ((src, dst) in IOCore.get().snapshotRedirectRules(userId, normalizedPackage)) {
                        builder.addIoRedirect(src, dst, false, 0)
                    }

                    val snapshot = builder.build()
                    // Never block install success on policy pipeline readiness.
                    if (ensurePolicyServicesReady()) {
                        val published = IOCore.get().getPolicyPublisher().publishNow(snapshot)
                        if (!published) {
                            policyState = "POLICY_FAILED"
                            policyError = "publish returned false"
                        }
                    } else {
                        policyState = "DEGRADED"
                        policyError = "policy services not ready"
                        IOCore.get().getPolicyPublisher().updatePolicy(snapshot)
                    }
                }.onFailure {
                    policyState = "POLICY_FAILED"
                    policyError = it.message ?: it.javaClass.simpleName
                    Slog.e(TAG, "Policy publish failed for $normalizedPackage (state=$policyState)", it)
                }

                if (policyState != "OK") {
                    val msg = if (policyError.isNullOrBlank()) {
                        "install succeeded; state=$policyState"
                    } else {
                        "install succeeded; state=$policyState; reason=$policyError"
                    }
                    result.msg = msg
                    Slog.w(TAG, "Install completed in degraded policy mode: $msg")
                } else if (!coreReady && result.msg.isNullOrBlank()) {
                    result.msg = "install succeeded after deferred core warm-up"
                }

                result
            }
        } catch (t: TimeoutCancellationException) {
            Slog.e(TAG, "Install timeout for package: $normalizedPackage", t)
            InstallResult().installError("Install timed out")
        } catch (t: Throwable) {
            Slog.e(TAG, "Install failed for package: $normalizedPackage", t)
            InstallResult().installError(t.message ?: "Install failed")
        } finally {
            clearPendingPackage(userId, normalizedPackage)
            notifyVirtualAppsChanged()
        }
    }

    @JvmStatic
    fun installAppBlocking(packageName: String, userId: Int = DEFAULT_USER_ID): InstallResult {
        return runBlocking { installApp(packageName, userId) }
    }

    suspend fun launchApp(packageName: String, userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(LAUNCH_TIMEOUT_MS) {
                ensureCoreServicesReady()
                // Policy bind is best-effort; launch must remain available via core path.
                runCatching { ensurePolicyServicesReady() }
                PrismSpaceCore.get().launchApk(packageName, userId)
            }
        } catch (t: Throwable) {
            Slog.e(TAG, "Launch failed for $packageName", t)
            false
        }
    }

    @JvmStatic fun clearAppData(packageName: String, userId: Int) = runCatching {
        PrismSpaceCore.get().clearPackage(packageName, userId)
        true
    }.getOrDefault(false)

    @JvmStatic fun uninstallApp(packageName: String, userId: Int) = runCatching {
        PrismSpaceCore.get().uninstallPackageAsUser(packageName, userId)
        notifyVirtualAppsChanged()
        true
    }.getOrDefault(false)

    @JvmStatic fun stopApp(packageName: String, userId: Int) = runCatching { PrismSpaceCore.get().stopPackage(packageName, userId); true }.getOrDefault(false)

    suspend fun forceStopApp(packageName: String, userId: Int = DEFAULT_USER_ID): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            ensureCoreServicesReady()
            PrismSpaceCore.get().forceColdStop(packageName, userId)
            true
        }.getOrElse { false }
    }

    suspend fun isAppRunning(packageName: String, userId: Int = DEFAULT_USER_ID): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            ensureCoreServicesReady()
            PrismSpaceCore.isRunningApplication(packageName, userId)
        }.getOrElse { false }
    }

    suspend fun stopAllRunningApps(userId: Int = DEFAULT_USER_ID): Int = withContext(Dispatchers.IO) {
        try {
            ensureCoreServicesReady()
            val apps = PrismSpaceCore.get().getInstalledApplications(PackageManager.GET_META_DATA, userId)
            var count = 0
            for (app in apps) {
                PrismSpaceCore.get().forceColdStop(app.packageName, userId)
                count++
            }
            count
        } catch (t: Throwable) { 0 }
    }

    suspend fun clearAllAppData(userId: Int = DEFAULT_USER_ID): Int = withContext(Dispatchers.IO) {
        try {
            ensureCoreServicesReady()
            val apps = PrismSpaceCore.get().getInstalledApplications(PackageManager.GET_META_DATA, userId)
            var count = 0
            for (app in apps) {
                PrismSpaceCore.get().clearPackage(app.packageName, userId)
                count++
            }
            count
        } catch (t: Throwable) { 0 }
    }

    suspend fun setVirtualLocation(packageName: String, lat: Double, lon: Double, userId: Int = DEFAULT_USER_ID): Boolean = withContext(Dispatchers.IO) {
        try {
            ensureCoreServicesReady()
            PLocationManager.get().setPattern(userId, packageName, PLocationManager.OWN_MODE)
            PLocationManager.get().setLocation(userId, packageName, PLocation(lat, lon))
            true
        } catch (t: Throwable) { false }
    }

    suspend fun generateRandomDeviceProfile(): DeviceProfileManager.DeviceProfile = withContext(Dispatchers.IO) {
        DeviceProfileManager.generateRandomProfile()
    }

    suspend fun saveDeviceProfile(packageName: String, profile: DeviceProfileManager.DeviceProfile, userId: Int = DEFAULT_USER_ID): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            ensureCoreServicesReady()
            DeviceProfileManager.saveProfile(packageName, userId, profile)
            true
        }.getOrElse { false }
    }

    suspend fun getVirtualizedAppIconBitmap(packageName: String, userId: Int = DEFAULT_USER_ID): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val hostPm = PrismSpaceCore.getPackageManager()
            val drawable = hostPm.getApplicationIcon(packageName)
            drawableToBitmap(drawable)
        }.getOrNull()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 192
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 192
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && 
            drawable.javaClass.name.contains("AdaptiveIconDrawable")) {
            runCatching {
                val bg = drawable.javaClass.getMethod("getBackground").invoke(drawable) as? Drawable
                val fg = drawable.javaClass.getMethod("getForeground").invoke(drawable) as? Drawable
                bg?.let {
                    it.setBounds(0, 0, canvas.width, canvas.height)
                    it.draw(canvas)
                }
                fg?.let {
                    it.setBounds(0, 0, canvas.width, canvas.height)
                    it.draw(canvas)
                }
            }.onFailure { drawable.draw(canvas) }
        } else {
            drawable.draw(canvas)
        }
        return bitmap
    }
}
