package com.prismspace.container.app

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Nullable
import com.prismspace.container.PrismSpaceCore
import com.prismspace.container.R
import com.prismspace.container.utils.Slog

class LauncherActivity : Activity() {
    private var isRunning = false

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)

            val intent = intent
            if (intent == null) {
                Slog.w(TAG, "Intent is null, finishing activity")
                finish()
                return
            }

            @Suppress("DEPRECATION")
            val launchIntent: Intent? = intent.getParcelableExtra(KEY_INTENT)
            val packageName: String? = intent.getStringExtra(KEY_PKG)
            val userId = intent.getIntExtra(KEY_USER_ID, 0)

            if (launchIntent == null || packageName == null) {
                Slog.w(TAG, "Missing launch intent or package name, finishing activity")
                finish()
                return
            }

            Slog.d(TAG, "LauncherActivity.onCreate() for package: $packageName, userId: $userId")

            val packageInfo = getPackageInfoWithFallback(packageName, userId)
            if (packageInfo == null) {
                Slog.w(TAG, "Package info not available for $packageName, but proceeding with launch")
            } else {
                Slog.d(TAG, "Successfully retrieved package info for $packageName")
            }

            var drawable: Drawable? = null
            var appName = packageName
            try {
                val appInfo = packageInfo?.applicationInfo
                if (appInfo != null) {
                    val pm: PackageManager = packageManager
                    drawable = pm.getApplicationIcon(appInfo)
                    val label = pm.getApplicationLabel(appInfo)
                    if (label != null) {
                        appName = label.toString()
                    }
                }
            } catch (e: Exception) {
                Slog.w(TAG, "Failed to load app icon or name for $packageName: ${e.message}")
            }

            setContentView(R.layout.activity_launcher)
            val iconView: ImageView? = findViewById(R.id.iv_icon)
            val nameView: TextView? = findViewById(R.id.tv_app_name)

            nameView?.apply {
                text = appName
                alpha = 0f
                animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(200)
                    .start()
            }

            if (iconView != null && drawable != null) {
                iconView.setImageDrawable(drawable)
                iconView.scaleX = 0.7f
                iconView.scaleY = 0.7f
                iconView.alpha = 0f
                iconView.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .alpha(1f)
                    .setDuration(350)
                    .setInterpolator(OvershootInterpolator())
                    .withEndAction {
                        iconView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .setInterpolator(DecelerateInterpolator())
                            .start()
                    }
                    .start()
            }

            launchAppAsync(launchIntent, userId)
        } catch (e: Exception) {
            Slog.e(TAG, "Critical error in LauncherActivity.onCreate()", e)
            finish()
        }
    }

    private fun getPackageInfoWithFallback(packageName: String, userId: Int): PackageInfo? {
        try {
            return PrismSpaceCore.getBPackageManager().getPackageInfo(packageName, 0, userId)
        } catch (e: Exception) {
            Slog.w(TAG, "Failed to get package info for $packageName (attempt 1): ${e.message}")

            try {
                return PrismSpaceCore.getBPackageManager()
                    .getPackageInfo(packageName, PackageManager.GET_META_DATA, userId)
            } catch (e2: Exception) {
                Slog.w(TAG, "Failed to get package info for $packageName (attempt 2): ${e2.message}")

                try {
                    val appInfo = PrismSpaceCore.getBPackageManager().getApplicationInfo(packageName, 0, userId)
                    if (appInfo != null) {
                        val fallbackInfo = PackageInfo()
                        fallbackInfo.packageName = packageName
                        fallbackInfo.applicationInfo = appInfo
                        @Suppress("DEPRECATION")
                        run {
                            fallbackInfo.versionCode = 1
                        }
                        fallbackInfo.versionName = "1.0"
                        fallbackInfo.firstInstallTime = System.currentTimeMillis()
                        fallbackInfo.lastUpdateTime = System.currentTimeMillis()
                        Slog.d(TAG, "Created fallback PackageInfo for $packageName")
                        return fallbackInfo
                    }
                } catch (e3: Exception) {
                    Slog.w(TAG, "Failed to get application info for $packageName: ${e3.message}")
                }
            }
        }
        return null
    }

    private fun launchAppAsync(launchIntent: Intent, userId: Int) {
        Thread({
            try {
                Slog.d(TAG, "Starting app launch in background thread")
                Thread.sleep(100)
                PrismSpaceCore.getBActivityManager().startActivity(launchIntent, userId)
                Slog.d(TAG, "App launch initiated successfully")
            } catch (e: Exception) {
                Slog.e(TAG, "Error launching app", e)
                runOnUiThread {
                    try {
                        Slog.e(TAG, "Failed to launch app: ${e.message}")
                    } catch (uiException: Exception) {
                        Slog.e(TAG, "Error showing error message", uiException)
                    }
                }
            }
        }, "AppLaunchThread").start()
    }

    override fun onPause() {
        super.onPause()
        isRunning = true
    }

    override fun onResume() {
        super.onResume()
        if (isRunning) {
            finish()
        }
    }

    companion object {
        @JvmField
        val TAG = "SplashScreen"

        @JvmField
        val KEY_INTENT = "launch_intent"

        @JvmField
        val KEY_PKG = "launch_pkg"

        @JvmField
        val KEY_USER_ID = "launch_user_id"

        @JvmStatic
        fun launch(intent: Intent?, userId: Int) {
            if (intent == null) {
                Slog.w(TAG, "Launch intent is null, skipping launcher activity")
                return
            }
            try {
                val splash = Intent()
                splash.setClass(PrismSpaceCore.getContext(), LauncherActivity::class.java)
                splash.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                splash.putExtra(KEY_INTENT, intent)
                splash.putExtra(KEY_PKG, intent.`package`)
                splash.putExtra(KEY_USER_ID, userId)
                PrismSpaceCore.getContext().startActivity(splash)
                Slog.d(TAG, "LauncherActivity.launch() called for package: ${intent.`package`}")
            } catch (e: Exception) {
                Slog.e(TAG, "Error in LauncherActivity.launch()", e)
            }
        }
    }
}

