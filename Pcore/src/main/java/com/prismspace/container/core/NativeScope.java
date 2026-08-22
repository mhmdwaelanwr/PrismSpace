package com.prismspace.container.core;

import androidx.annotation.NonNull;

/**
 * H5: Safe AutoCloseable wrapper for Binder Identity Spoofing.
 * Ensures that the thread-local scope depth is correctly balanced even if exceptions occur.
 * 
 * Usage:
 * try (NativeScope scope = NativeScope.enterAppScope()) {
 *     // Inside this block, getCallingUid() will return the spoofed UID if enforced.
 *     binder.onTransact(...);
 * }
 */
public final class NativeScope implements AutoCloseable {

    private NativeScope() {
        NativeCore.nativeEnterAppScope();
    }

    /**
     * Enters the application server transaction scope.
     * @return A Closeable scope object.
     */
    @NonNull
    public static NativeScope enterAppScope() {
        return new NativeScope();
    }

    @Override
    public void close() {
        NativeCore.nativeExitAppScope();
    }
}
