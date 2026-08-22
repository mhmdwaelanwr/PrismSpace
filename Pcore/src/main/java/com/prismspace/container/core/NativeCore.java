package com.prismspace.container.core;

import android.util.Log;
import androidx.annotation.Keep;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * H6: Unified Native Control Plane.
 */
@SuppressWarnings({"JavaJniMissingFunction", "unused"})
public class NativeCore {
    public static final String TAG = "NativeCore";

    private static final int HOOK_UNIX_FS = 1 << 0;
    private static final int HOOK_RUNTIME = 1 << 1;
    private static final int HOOK_RUNTIME_OBSERVE_ONLY = 1 << 2;
    private static final int HOOK_DEX = 1 << 3;
    private static final int HOOK_DEX_OBSERVE_ONLY = 1 << 4;
    private static final int HOOK_VM_CLASS_LOADER = 1 << 5;
    private static final int HOOK_BINDER = 1 << 6;
    private static final int REQUIRED_HOOK_MASK =
            HOOK_UNIX_FS | HOOK_RUNTIME | HOOK_DEX | HOOK_VM_CLASS_LOADER | HOOK_BINDER;

    private static final AtomicReference<Boolean> sIsReady = new AtomicReference<>(false);
    private static final AtomicBoolean sBootstrapStarted = new AtomicBoolean(false);
    private static final AtomicBoolean sIoEnabled = new AtomicBoolean(false);
    private static final ExecutorService sBootstrapExecutor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "PrismNativeBootstrap"));
    private static final Set<String> sRegisteredIoRules = ConcurrentHashMap.newKeySet();

    static {
        System.loadLibrary("prismspace");
    }

    //noinspection JavaJniMissingFunction
    public static native boolean nativeBootstrap(String tracePath);
    //noinspection JavaJniMissingFunction
    public static native int nativeReadHookStatus();
    //noinspection JavaJniMissingFunction
    public static native boolean nativeInstallPolicyFromFd(int fd, long declaredSize, long generation, int transport);
    //noinspection JavaJniMissingFunction
    public static native void nativeSetBinderMode(int mode);
    //noinspection JavaJniMissingFunction
    public static native long[] nativeReadBinderMetrics();
    //noinspection JavaJniMissingFunction
    public static native void nativeIncrementBinderMetric(int index);
    //noinspection JavaJniMissingFunction
    public static native void nativeEnterAppScope();
    //noinspection JavaJniMissingFunction
    public static native void nativeExitAppScope();
    //noinspection JavaJniMissingFunction
    public static native void nativeEnableIO();
    //noinspection JavaJniMissingFunction
    public static native void nativeAddIORule(String targetPath, String relocatePath);
    //noinspection JavaJniMissingFunction
    public static native boolean nativeDisableHiddenApi();
    //noinspection JavaJniMissingFunction
    public static native boolean nativeDisableResourceLoading();
    //noinspection JavaJniMissingFunction
    public static native void nativeNotifyMemoryPressure(int level);
    //noinspection JavaJniMissingFunction
    public static native void nativeSetAuditLogPath(String path);

    public static void ensureBootstrapped() {
        if (sIsReady.get()) return;
        if (!sBootstrapStarted.compareAndSet(false, true)) {
            return;
        }
        sBootstrapExecutor.execute(() -> {
            try {
                boolean bootstrapAlive = nativeBootstrap("");
                int statusMask = nativeReadHookStatus();
                publishNativeCapabilities(bootstrapAlive, statusMask);

                if (bootstrapAlive) {
                    sIsReady.set(true);
                    Log.i(TAG, "Native bootstrap completed with statusMask=" + statusMask);
                } else {
                    // A failed probe must be retryable. The old implementation left this latched.
                    sBootstrapStarted.set(false);
                    Log.e(TAG, "Native bootstrap failed: no native subsystem initialized");
                }
            } catch (Throwable t) {
                sBootstrapStarted.set(false);
                EngineCapabilities.get().mark(
                        EngineCapabilities.Component.NATIVE_BOOTSTRAP,
                        EngineCapabilities.State.FAILED,
                        t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage()));
                Log.e(TAG, "Native bootstrap critical failure", t);
            }
        });
    }

    private static void publishNativeCapabilities(boolean bootstrapAlive, int statusMask) {
        EngineCapabilities capabilities = EngineCapabilities.get();
        boolean complete = (statusMask & REQUIRED_HOOK_MASK) == REQUIRED_HOOK_MASK;

        capabilities.mark(
                EngineCapabilities.Component.NATIVE_BOOTSTRAP,
                !bootstrapAlive ? EngineCapabilities.State.FAILED
                        : complete ? EngineCapabilities.State.ACTIVE : EngineCapabilities.State.DEGRADED,
                "mask=0x" + Integer.toHexString(statusMask));

        markBinaryCapability(capabilities, EngineCapabilities.Component.UNIX_FS,
                (statusMask & HOOK_UNIX_FS) != 0, false);
        markBinaryCapability(capabilities, EngineCapabilities.Component.RUNTIME_LOAD,
                (statusMask & HOOK_RUNTIME) != 0,
                (statusMask & HOOK_RUNTIME_OBSERVE_ONLY) != 0);
        markBinaryCapability(capabilities, EngineCapabilities.Component.DEX_LOAD,
                (statusMask & HOOK_DEX) != 0,
                (statusMask & HOOK_DEX_OBSERVE_ONLY) != 0);
        markBinaryCapability(capabilities, EngineCapabilities.Component.VM_CLASS_LOADER,
                (statusMask & HOOK_VM_CLASS_LOADER) != 0, false);
        markBinaryCapability(capabilities, EngineCapabilities.Component.BINDER,
                (statusMask & HOOK_BINDER) != 0, false);
    }

    private static void markBinaryCapability(
            EngineCapabilities capabilities,
            EngineCapabilities.Component component,
            boolean installed,
            boolean observeOnly) {
        if (!installed) {
            capabilities.mark(component, EngineCapabilities.State.FAILED, "native install failed");
        } else if (observeOnly) {
            capabilities.mark(component, EngineCapabilities.State.DEGRADED, "observe-only");
        } else {
            capabilities.mark(component, EngineCapabilities.State.ACTIVE, "intercept active");
        }
    }

    public static boolean isReady() { return sIsReady.get(); }

    // Legacy-compatible entry points used by existing Java lifecycle code paths.
    public static void startupInit(int apiLevel) { ensureBootstrapped(); }

    public static void startupEnableIO() {
        ensureBootstrapped();
        if (!sIoEnabled.compareAndSet(false, true)) {
            return;
        }
        try {
            nativeEnableIO();
        } catch (Throwable t) {
            sIoEnabled.set(false);
            EngineCapabilities.get().mark(
                    EngineCapabilities.Component.UNIX_FS,
                    EngineCapabilities.State.FAILED,
                    "enable IO failed: " + t.getClass().getSimpleName());
            Log.e(TAG, "startupEnableIO failed", t);
        }
    }

    public static void startupAddIORule(String targetPath, String relocatePath) {
        if (targetPath == null || relocatePath == null) return;
        ensureBootstrapped();
        final String key = targetPath + "->" + relocatePath;
        if (!sRegisteredIoRules.add(key)) {
            return;
        }
        try {
            nativeAddIORule(targetPath, relocatePath);
        } catch (Throwable t) {
            sRegisteredIoRules.remove(key);
            Log.e(TAG, "startupAddIORule failed", t);
        }
    }

    public static boolean startupDisableHiddenApi() {
        ensureBootstrapped();
        try {
            boolean enabled = nativeDisableHiddenApi();
            EngineCapabilities.get().mark(
                    EngineCapabilities.Component.HIDDEN_API,
                    enabled ? EngineCapabilities.State.ACTIVE : EngineCapabilities.State.DEGRADED,
                    enabled ? "native exemption active" : "native bypass unavailable; Java fallback may be attempted");
            return enabled;
        } catch (Throwable t) {
            EngineCapabilities.get().mark(
                    EngineCapabilities.Component.HIDDEN_API,
                    EngineCapabilities.State.DEGRADED,
                    "native bypass error: " + t.getClass().getSimpleName());
            Log.e(TAG, "startupDisableHiddenApi failed", t);
            return false;
        }
    }

    public static boolean startupDisableResourceLoading() {
        ensureBootstrapped();
        try {
            return nativeDisableResourceLoading();
        } catch (Throwable t) {
            Log.e(TAG, "startupDisableResourceLoading failed", t);
            return false;
        }
    }

    public static void notifyMemoryPressure(int level) {
        try {
            nativeNotifyMemoryPressure(level);
        } catch (Throwable t) {
            Log.e(TAG, "notifyMemoryPressure failed", t);
        }
    }

    public static void setAuditLogPath(String path) {
        try {
            nativeSetAuditLogPath(path);
        } catch (Throwable t) {
            Log.e(TAG, "setAuditLogPath failed", t);
        }
    }

    public static boolean isNativeDisabledForDiagnostics() { return false; }

    public static void installPolicy(ByteBuffer blob, long generation) {
        // Deliberately unused in hybrid mode: policy is delivered through PolicyPublisher.
    }

    @Keep
    public static int getCallingUid(int origCallingUid) { return origCallingUid; }

    @Keep
    public static String redirectPath(String path) {
        return IOCore.get().redirectPath(path);
    }

    @Keep
    public static List<Long> loadEmptyDex() { return new ArrayList<>(); }
}
