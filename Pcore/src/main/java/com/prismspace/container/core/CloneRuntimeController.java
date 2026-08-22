package com.prismspace.container.core;

import android.util.Log;

/**
 * H5: Runtime Controller for Binder Identity Spoofing.
 * Manages the transition between Shadow and Enforce modes and tracks security metrics.
 */
public final class CloneRuntimeController {
    private static final String TAG = "CloneRuntimeController";

    public static final int MODE_SHADOW = 0;
    public static final int MODE_ENFORCE = 1;

    public static class Metrics {
        public long totalCalls;
        public long shadowCandidates;
        public long wouldSpoofOutsideScope;
        public long wouldSpoofSystemUid;
        public long wouldSpoofSelfUid;
        public long validationFailures;
        public long rollbackRejections;
        public long securityExceptionCorrelated;
        public long remoteExceptionCorrelated;
        public long crashCorrelated;
        public long systemUidCalls;
        public long normalAppUidCalls;
        public long selfUidCalls;
        public long currentGeneration;
        public long lastModeChangeElapsedMs;

        public Metrics(long[] raw) {
            if (raw == null || raw.length < 15) return;
            this.totalCalls = raw[0];
            this.shadowCandidates = raw[1];
            this.wouldSpoofOutsideScope = raw[2];
            this.wouldSpoofSystemUid = raw[3];
            this.wouldSpoofSelfUid = raw[4];
            this.validationFailures = raw[5];
            this.rollbackRejections = raw[6];
            this.securityExceptionCorrelated = raw[7];
            this.remoteExceptionCorrelated = raw[8];
            this.crashCorrelated = raw[9];
            this.systemUidCalls = raw[10];
            this.normalAppUidCalls = raw[11];
            this.selfUidCalls = raw[12];
            this.currentGeneration = raw[13];
            this.lastModeChangeElapsedMs = raw[14];
        }

        @Override
        public String toString() {
            return "BinderMetrics{" +
                    "total=" + totalCalls +
                    ", shadow=" + shadowCandidates +
                    ", sys_hits=" + wouldSpoofSystemUid +
                    ", self_hits=" + selfUidCalls +
                    ", out_scope=" + wouldSpoofOutsideScope +
                    ", sec_exceptions=" + securityExceptionCorrelated +
                    ", gen=" + currentGeneration +
                    '}';
        }
    }

    public static void setBinderMode(int mode) {
        Log.i(TAG, "Updating Binder Spoofing Mode to: " + (mode == MODE_ENFORCE ? "ENFORCE" : "SHADOW"));
        NativeCore.nativeSetBinderMode(mode);
    }

    public static Metrics readMetrics() {
        long[] raw = NativeCore.nativeReadBinderMetrics();
        return new Metrics(raw);
    }

    public static void reportSecurityException() {
        // [7] security_exception_correlated
        NativeCore.nativeIncrementBinderMetric(7);
    }

    public static void reportCrash() {
        // [9] crash_correlated
        NativeCore.nativeIncrementBinderMetric(9);
    }
}
