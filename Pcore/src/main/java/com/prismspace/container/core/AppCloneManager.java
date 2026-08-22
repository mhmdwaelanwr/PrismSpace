package com.prismspace.container.core;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.entity.pm.InstallResult;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * H6: AppCloneManager - Fixed for compatibility.
 */
public final class AppCloneManager {
    private static final String TAG = "AppCloneManager";
    private static final AppCloneManager sInstance = new AppCloneManager(PrismSpaceCore.getContext());

    public interface Listener {
        @MainThread void onCloneRecordChanged(@NonNull AppCloneRecord record);
        @MainThread void onCloneOperationFailed(@NonNull String packageName, @NonNull Throwable error);
    }

    private final Context appContext;
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Set<Listener> listeners = ConcurrentHashMap.newKeySet();
    private final Set<String> inFlightPackages = ConcurrentHashMap.newKeySet();

    public static AppCloneManager get() { return sInstance; }

    private AppCloneManager(Context context) {
        this.appContext = context != null ? context.getApplicationContext() : null;
    }

    public void addListener(@NonNull Listener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void createClone(@NonNull String packageName, int userId) {
        if (!inFlightPackages.add(packageName)) return;

        dispatchRecordChanged(AppCloneRecord.creating(userId, packageName, packageName, packageName));

        backgroundExecutor.execute(() -> {
            try {
                PackageManager pm = appContext.getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
                String label = pm.getApplicationLabel(info).toString();

                dispatchRecordChanged(AppCloneRecord.creating(userId, packageName, label, packageName));

                // Single owner: PrismEngineFacade drives install -> snapshot -> publish.
                InstallResult result = PrismEngineFacade.installAppBlocking(packageName, userId);
                if (!result.success) {
                    throw new RuntimeException("Installation failed: " + result.msg);
                }

                long gen = System.currentTimeMillis();
                AppCloneRecord readyRecord = new AppCloneRecord(
                        userId, packageName, label, packageName,
                        CloneStatus.READY, PolicyState.PUBLISHED, gen
                );
                dispatchRecordChanged(readyRecord);

            } catch (Throwable t) {
                Log.e(TAG, "Failed to create clone", t);
                dispatchFailure(packageName, t);
            } finally {
                inFlightPackages.remove(packageName);
            }
        });
    }

    private void dispatchRecordChanged(AppCloneRecord record) {
        mainHandler.post(() -> {
            for (Listener l : listeners) l.onCloneRecordChanged(record);
        });
    }

    private void dispatchFailure(String pkg, Throwable t) {
        mainHandler.post(() -> {
            for (Listener l : listeners) l.onCloneOperationFailed(pkg, t);
        });
    }

    public enum CloneStatus { CREATING, READY, ERROR }
    public enum PolicyState { NOT_BUILT, PUBLISHED, FAILED }

    public static final class AppCloneRecord {
        public final int cloneId;
        public final String packageName;
        public final String displayName;
        public final String iconResName;
        public final CloneStatus status;
        public final PolicyState policyState;
        public final long generation;

        public AppCloneRecord(int id, String pkg, String name, String icon, CloneStatus s, PolicyState p, long gen) {
            this.cloneId = id; this.packageName = pkg; this.displayName = name;
            this.iconResName = icon; this.status = s; this.policyState = p; this.generation = gen;
        }

        public static AppCloneRecord creating(int id, String pkg, String name, String icon) {
            return new AppCloneRecord(id, pkg, name, icon, CloneStatus.CREATING, PolicyState.NOT_BUILT, -1);
        }
    }
}
