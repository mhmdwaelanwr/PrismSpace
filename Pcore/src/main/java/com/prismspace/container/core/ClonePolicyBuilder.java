package com.prismspace.container.core;

import android.util.Log;
import androidx.annotation.NonNull;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * H5/H6: Updated Builder to match AppCloneManager requirements.
 */
public class ClonePolicyBuilder {
    private static final String TAG = "ClonePolicyBuilder";
    private final PolicySnapshotBuilder mBaseBuilder = new PolicySnapshotBuilder();
    private int mCloneId;
    private String mPackageName;
    private final long mGeneration;
    private final java.util.List<ClonePolicySnapshot.IoRule> mIoRules = new java.util.ArrayList<>();
    private final java.util.List<ClonePolicySnapshot.BinderRule> mBinderRules = new java.util.ArrayList<>();

    public ClonePolicyBuilder(long generation) {
        this.mGeneration = generation;
    }

    public void setCloneId(int cloneId) {
        this.mCloneId = cloneId;
    }

    public void setPackageName(@NonNull String packageName) {
        this.mPackageName = packageName;
    }

    public void addIoRedirect(String src, String dst, boolean exact, int precedence) {
        mBaseBuilder.addRule(src, dst, exact, precedence);
        mIoRules.add(new ClonePolicySnapshot.IoRule(src, dst, exact, precedence));
    }

    public void addBinderSpoof(int originalAppId, int spoofAppId) {
        if (originalAppId < 10000 || originalAppId >= 20000 || spoofAppId < 10000 || spoofAppId >= 20000) {
            Log.w(TAG, "Invalid AppId spoofing: " + originalAppId + " -> " + spoofAppId);
            return;
        }
        mBaseBuilder.addBinderSpoof(originalAppId, spoofAppId);
        mBinderRules.add(new ClonePolicySnapshot.BinderRule(originalAppId, spoofAppId));
    }

    public ClonePolicySnapshot build() {
        // Keep validation/shape compatibility with existing blob builder.
        mBaseBuilder.build(mGeneration);
        ClonePolicySnapshot snapshot = new ClonePolicySnapshot(mGeneration);
        snapshot.ioRules.addAll(mIoRules);
        snapshot.binderRules.addAll(mBinderRules);
        return snapshot;
    }
}
