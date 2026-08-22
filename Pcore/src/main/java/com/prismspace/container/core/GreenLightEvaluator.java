package com.prismspace.container.core;

import android.util.Log;

/**
 * H5: Decision engine for promoting Binder Identity Spoofing from Shadow to Enforce.
 * Implements "Safety First" automation based on real-time metrics.
 */
public class GreenLightEvaluator {
    private static final String TAG = "GreenLight";

    public enum State {
        SHADOW_WARMUP,   // Collecting metrics, no spoofing.
        CANARY_ACTIVE,   // Limited enforcement.
        FULL_ENFORCE,    // All rules applied.
        ROLLBACK         // Emergency shutdown due to detected instability.
    }

    private State mCurrentState = State.SHADOW_WARMUP;
    private int mEnforceCount = 0;

    public synchronized void evaluate() {
        CloneRuntimeController.Metrics metrics = CloneRuntimeController.readMetrics();
        
        // 1. Safety Guard: Immediate Rollback if anomalies detected
        if (metrics.securityExceptionCorrelated > 0 || metrics.crashCorrelated > 0) {
            if (mCurrentState != State.ROLLBACK) {
                Log.e(TAG, "EMERGENCY ROLLBACK: Instability detected! Exceptions=" + 
                      metrics.securityExceptionCorrelated + " Crashes=" + metrics.crashCorrelated);
                rollback();
            }
            return;
        }

        // 2. Promotion Logic
        switch (mCurrentState) {
            case SHADOW_WARMUP:
                // Require at least 100 valid candidates and < 2% outside scope transactions
                if (metrics.shadowCandidates > 100) {
                    double leakRate = (double) metrics.wouldSpoofOutsideScope / metrics.shadowCandidates;
                    if (leakRate < 0.02) {
                        Log.i(TAG, "WARMUP SUCCESS: Promoting to CANARY_ACTIVE. Leak Rate: " + (leakRate * 100) + "%");
                        mCurrentState = State.CANARY_ACTIVE;
                        CloneRuntimeController.setBinderMode(CloneRuntimeController.MODE_ENFORCE);
                    }
                }
                break;

            case CANARY_ACTIVE:
                // Stay in Canary for some time or until high confidence
                if (metrics.selfUidCalls > 500 && metrics.totalCalls > 2000) {
                    Log.i(TAG, "STABILITY CONFIRMED: Promoting to FULL_ENFORCE.");
                    mCurrentState = State.FULL_ENFORCE;
                }
                break;

            case FULL_ENFORCE:
                // Steady state
                break;
                
            case ROLLBACK:
                // Do nothing until manual reset
                break;
        }
    }

    private void rollback() {
        mCurrentState = State.ROLLBACK;
        CloneRuntimeController.setBinderMode(CloneRuntimeController.MODE_SHADOW);
        // Record the failure in metrics
        NativeCore.nativeIncrementBinderMetric(6); // rollback_rejections
    }

    public State getCurrentState() {
        return mCurrentState;
    }
}
