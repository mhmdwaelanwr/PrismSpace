package com.prismspace.container.core;

import android.os.Build;
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
import com.prismspace.container.PrismSpaceCore;

/**
 * H6: Unified Native Control Plane.
 */
@SuppressWarnings({"JavaJniMissingFunction", "unused"})
public class NativeCore {
    public static final String TAG = "NativeCore";

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
                if (nativeBootstrap("")) {
                    sIsReady.set(true);
                    Log.i(TAG, "Native bootstrap successful");
                }
            } catch (Throwable t) {
                sBootstrapStarted.set(false);
                Log.e(TAG, "Native bootstrap critical failure", t);
            }
        });
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
            return nativeDisableHiddenApi();
        } catch (Throwable t) {
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
