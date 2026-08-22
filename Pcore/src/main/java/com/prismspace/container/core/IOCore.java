package com.prismspace.container.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.core.env.PEnvironment;
import com.prismspace.container.utils.FileUtils;
import com.prismspace.container.utils.TrieTree;


@SuppressLint("SdCardPath")
public class IOCore {
    public static final String TAG = "IOCore";
    private static final int REDIRECT_CACHE_SIZE = 256;

    private static final IOCore sIOCore = new IOCore();
    private static final TrieTree mTrieTree = new TrieTree();
    private static final TrieTree sBlackTree = new TrieTree();
    private final Map<String, String> mRedirectMap = new LinkedHashMap<>();
    
    private final Map<String, String> mPathRedirectCache = new LinkedHashMap<String, String>(REDIRECT_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > REDIRECT_CACHE_SIZE;
        }
    };

    private long mPolicyGeneration = 0;
    private final PolicyPublisher mPolicyPublisher;
    private final java.util.Set<String> mRedirectEnabledPackages = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String, Boolean>());
    private volatile int mLastPublishedRuleCount = -1;

    private IOCore() {
        mPolicyPublisher = new PolicyPublisher(new PolicyPublisher.DefaultBlobCompiler());
    }

    public static IOCore get() {
        return sIOCore;
    }

    public PolicyPublisher getPolicyPublisher() {
        return mPolicyPublisher;
    }

    private void clearRedirectCaches() {
        synchronized (mPathRedirectCache) {
            mPathRedirectCache.clear();
        }
    }

    public void addRedirect(String origPath, String redirectPath) {
        if (TextUtils.isEmpty(origPath) || TextUtils.isEmpty(redirectPath) || mRedirectMap.get(origPath) != null)
            return;

        mTrieTree.add(origPath);
        synchronized (mRedirectMap) {
            mRedirectMap.put(origPath, redirectPath);
        }
        File redirectFile = new File(redirectPath);
        if (!redirectFile.exists()) {
            FileUtils.mkdirs(redirectPath);
        }

        // Keep native IO redirect table hot while H5.2 policy publishing remains the source of truth.
        NativeCore.startupAddIORule(origPath, redirectPath);
        clearRedirectCaches();
    }

    public void addBlackRedirect(String path) {
        if (TextUtils.isEmpty(path))
            return;
        sBlackTree.add(path);
        clearRedirectCaches();
    }

    public void publishPolicy() {
        ClonePolicySnapshot snapshot = new ClonePolicySnapshot(++mPolicyGeneration);

        synchronized (mRedirectMap) {
            for (Map.Entry<String, String> entry : mRedirectMap.entrySet()) {
                snapshot.ioRules.add(new ClonePolicySnapshot.IoRule(entry.getKey(), entry.getValue(), false, 0));
            }
        }

        if (snapshot.ioRules.size() == mLastPublishedRuleCount) {
            Log.d(TAG, "publishPolicy skipped: same redirect rule count=" + mLastPublishedRuleCount);
            return;
        }

        try {
            mPolicyPublisher.updatePolicy(snapshot);
            mLastPublishedRuleCount = snapshot.ioRules.size();
            Log.i(TAG, "Policy updated and scheduled for publish. Generation: " + mPolicyGeneration);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update policy in publisher", e);
        }
    }

    public Map<String, String> snapshotRedirectRules() {
        synchronized (mRedirectMap) {
            return new LinkedHashMap<>(mRedirectMap);
        }
    }

    public Map<String, String> snapshotRedirectRules(int cloneId, String packageName) {
        Map<String, String> cloneRules = new LinkedHashMap<>();

        // Clone-specific workspace paths are always included and override any global path alias.
        String virtualDataDir = PEnvironment.getDataDir(packageName, cloneId).getAbsolutePath();
        String virtualDeDataDir = PEnvironment.getDeDataDir(packageName, cloneId).getAbsolutePath();
        String virtualExternalDir = PEnvironment.getExternalDataDir(packageName, cloneId).getAbsolutePath();
        String virtualExternalUserRoot = PEnvironment.getExternalUserDir(cloneId).getAbsolutePath();

        cloneRules.put(String.format("/data/data/%s", packageName), virtualDataDir);
        cloneRules.put(String.format("/data/user/%d/%s", cloneId, packageName), virtualDataDir);
        cloneRules.put(String.format("/data/user_de/%d/%s", cloneId, packageName), virtualDeDataDir);
        cloneRules.put(String.format("/storage/emulated/%d/Android/data/%s", cloneId, packageName), virtualExternalDir);
        cloneRules.put(String.format("/sdcard/Android/data/%s", packageName), virtualExternalDir);
        cloneRules.put(String.format("/storage/emulated/%d", cloneId), virtualExternalUserRoot);

        synchronized (mRedirectMap) {
            for (Map.Entry<String, String> entry : mRedirectMap.entrySet()) {
                cloneRules.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        return cloneRules;
    }

    public String redirectPath(String path) {
        if (TextUtils.isEmpty(path))
            return path;
        
        final String originalPath = path;
        synchronized (mPathRedirectCache) {
            String cached = mPathRedirectCache.get(originalPath);
            if (cached != null) {
                return cached;
            }
        }
        
        if (path.contains("/prismspace/")) {
            return path;
        }

        String search = sBlackTree.search(path);
        if (!TextUtils.isEmpty(search)) {
            return search;
        }

        String key = mTrieTree.search(path);
        if (!TextUtils.isEmpty(key))
            path = path.replace(key, Objects.requireNonNull(mRedirectMap.get(key)));

        synchronized (mPathRedirectCache) {
            mPathRedirectCache.put(originalPath, path);
        }

        return path;
    }

    public File redirectPath(File path) {
        if (path == null)
            return null;
        return new File(redirectPath(path.getAbsolutePath()));
    }

    public void enableRedirect(Context context) {
        Map<String, String> rule = new LinkedHashMap<>();
        String packageName = context.getPackageName();

        if (!mRedirectEnabledPackages.add(packageName)) {
            Log.d(TAG, "enableRedirect skipped for already initialized package=" + packageName);
            return;
        }

        try {
            ApplicationInfo packageInfo = PrismSpaceCore.getBPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA, PrismSpaceCore.getUserId());
            int systemUserId = PrismSpaceCore.getHostUserId();
            
            rule.put(String.format("/data/data/%s/lib", packageName), packageInfo.nativeLibraryDir);
            rule.put(String.format("/data/user/%d/%s/lib", systemUserId, packageName), packageInfo.nativeLibraryDir);

            rule.put(String.format("/data/data/%s", packageName), packageInfo.dataDir);
            rule.put(String.format("/data/user/%d/%s", systemUserId, packageName), packageInfo.dataDir);

            File profilesRoot = new File(PEnvironment.getVirtualRoot(), "profiles");
            FileUtils.mkdirs(profilesRoot.getAbsolutePath());
            rule.put("/data/misc/profiles", profilesRoot.getAbsolutePath());

            if (PrismSpaceCore.getContext().getExternalCacheDir() != null) {
                File external = PEnvironment.getExternalUserDir(PrismSpaceCore.getUserId());
                rule.put("/sdcard", external.getAbsolutePath());
                rule.put(String.format("/storage/emulated/%d", systemUserId), external.getAbsolutePath());
            }
            
            if (PrismSpaceCore.get().isHideRoot()) {
                hideRoot(rule);
            }
            proc(rule);
        } catch (Exception e) {
            Log.e(TAG, "enableRedirect failed", e);
        }

        for (Map.Entry<String, String> entry : rule.entrySet()) {
            addRedirect(entry.getKey(), entry.getValue());
        }

        NativeCore.startupEnableIO();
        publishPolicy();
    }

    private void hideRoot(Map<String, String> rule) {
        rule.put("/system/app/Superuser.apk", "/system/app/Superuser.apk-fake");
        rule.put("/sbin/su", "/sbin/su-fake");
        rule.put("/system/bin/su", "/system/bin/su-fake");
        rule.put("/system/xbin/su", "/system/xbin/su-fake");
    }

    private void proc(Map<String, String> rule) {
        int appPid = PrismSpaceCore.getAppPid();
        int pid = Process.myPid();
        String selfProc = "/proc/self/";
        String proc = "/proc/" + pid + "/";

        String cmdline = new File(PEnvironment.getProcDir(appPid), "cmdline").getAbsolutePath();
        rule.put(proc + "cmdline", cmdline);
        rule.put(selfProc + "cmdline", cmdline);
    }
}
