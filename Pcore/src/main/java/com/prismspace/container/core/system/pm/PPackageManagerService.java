package com.prismspace.container.core.system.pm;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.RemoteException;
import android.text.TextUtils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.core.GmsCore;
import com.prismspace.container.core.env.PEnvironment;
import com.prismspace.container.core.system.PProcessManagerService;
import com.prismspace.container.core.system.ISystemService;
import com.prismspace.container.core.system.ProcessRecord;
import com.prismspace.container.core.system.am.PActivityManagerService;
import com.prismspace.container.core.system.user.PUserHandle;
import com.prismspace.container.core.system.user.PUserInfo;
import com.prismspace.container.core.system.user.PUserManagerService;
import com.prismspace.container.entity.pm.InstallOption;
import com.prismspace.container.entity.pm.InstallResult;
import com.prismspace.container.entity.pm.InstalledPackage;
import com.prismspace.container.utils.AbiUtils;
import com.prismspace.container.utils.FileUtils;
import com.prismspace.container.utils.Slog;
import com.prismspace.container.utils.compat.PackageParserCompat;


import static android.content.pm.PackageManager.MATCH_DIRECT_BOOT_UNAWARE;



public class PPackageManagerService extends IBPackageManagerService.Stub implements ISystemService {
    public static final String TAG = "PPackageManagerService";
    public static PPackageManagerService sService = new PPackageManagerService();
    private final Settings mSettings = new Settings();
    private final ComponentResolver mComponentResolver;
    private static final PUserManagerService sUserManager = PUserManagerService.get();
    private final List<PackageMonitor> mPackageMonitors = new ArrayList<>();

    final Map<String, PPackageSettings> mPackages = mSettings.mPackages;
    final Object mInstallLock = new Object();

    public static PPackageManagerService get() {
        return sService;
    }

    public PPackageManagerService() {
        mComponentResolver = new ComponentResolver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        PrismSpaceCore.getContext()
                .registerReceiver(mPackageChangedHandler, filter);
    }

    private final BroadcastReceiver mPackageChangedHandler = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!TextUtils.isEmpty(action)) {
                if ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_REMOVED".equals(action)) {
                    mSettings.scanPackage();
                }
            }
        }
    };

    @Override
    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
        if (!sUserManager.exists(userId)) return null;
        if (Objects.equals(packageName, PrismSpaceCore.getHostPkg())) {
            try {
                return PrismSpaceCore.getPackageManager().getApplicationInfo(packageName, flags);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
        flags = updateFlags(flags, userId);
        
        synchronized (mPackages) {
            
            PPackageSettings ps = mPackages.get(packageName);
            if (ps != null) {
                PPackage p = ps.pkg;
                return PackageManagerCompat.generateApplicationInfo(p, flags, ps.readUserState(userId), userId);
            }
        }
        return null;
    }

    @Override
    public ResolveInfo resolveService(Intent intent, int flags, String resolvedType, int userId) {
        if (!sUserManager.exists(userId)) return null;
        List<ResolveInfo> query = queryIntentServicesInternal(
                intent, resolvedType, flags, userId);
        if (query != null) {
            if (query.size() >= 1) {
                
                
                return query.get(0);
            }
        }
        return null;
    }

    private List<ResolveInfo> queryIntentServicesInternal(Intent intent, String resolvedType, int flags, int userId) {
        ComponentName comp = intent.getComponent();
        if (comp == null) {
            if (intent.getSelector() != null) {
                intent = intent.getSelector();
                comp = intent.getComponent();
            }
        }
        if (comp != null) {
            final List<ResolveInfo> list = new ArrayList<>(1);
            final ServiceInfo si = getServiceInfo(comp, flags, userId);
            if (si != null) {
                
                
                
                
                final ResolveInfo ri = new ResolveInfo();
                ri.serviceInfo = si;
                list.add(ri);
            }
            return list;
        }

        
        synchronized (mPackages) {
            String pkgName = intent.getPackage();
            if (pkgName != null) {
                PPackageSettings bPackageSettings = mPackages.get(pkgName);
                if (bPackageSettings != null) {
                    final PPackage pkg = bPackageSettings.pkg;
                    return mComponentResolver.queryServices(intent, resolvedType, flags, pkg.services,
                            userId);
                }
            } else {
               return mComponentResolver.queryServices(intent, resolvedType, flags, userId);
            }
            return Collections.emptyList();
        }
    }

    @Override
    public ResolveInfo resolveActivity(Intent intent, int flags, String resolvedType, int userId) {
        if (!sUserManager.exists(userId)) return null;
        List<ResolveInfo> resolves = queryIntentActivities(intent, resolvedType, flags, userId);
        return chooseBestActivity(intent, resolvedType, flags, resolves);
    }

    @Override
    public ProviderInfo resolveContentProvider(String authority, int flags, int userId) {
        if (!sUserManager.exists(userId)) return null;
        return mComponentResolver.queryProvider(authority, flags, userId);
    }

    @Override
    public ResolveInfo resolveIntent(Intent intent, String resolvedType, int flags, int userId) {
        if (!sUserManager.exists(userId)) return null;
        List<ResolveInfo> resolves = queryIntentActivities(intent, resolvedType, flags, userId);
        return chooseBestActivity(intent, resolvedType, flags, resolves);
    }

    private ResolveInfo chooseBestActivity(Intent intent, String resolvedType,
                                           int flags, List<ResolveInfo> query) {
        if (query != null) {
            final int N = query.size();
            if (N == 1) {
                return query.get(0);
            } else if (N > 1) {
                
                
                ResolveInfo r0 = query.get(0);
                ResolveInfo r1 = query.get(1);
                
                
                if (r0.priority != r1.priority
                        || r0.preferredOrder != r1.preferredOrder
                        || r0.isDefault != r1.isDefault) {
                    return query.get(0);
                }
            }
        }
        return null;
    }

    private List<ResolveInfo> queryIntentActivities(Intent intent,
                                                    String resolvedType, int flags, int userId) {
        ComponentName comp = intent.getComponent();
        if (comp == null) {
            if (intent.getSelector() != null) {
                intent = intent.getSelector();
                comp = intent.getComponent();
            }
        }

        if (comp != null) {
            final List<ResolveInfo> list = new ArrayList<>(1);
            final ActivityInfo ai = getActivity(comp, flags, userId);
            if (ai != null) {
                
                
                
                
                final ResolveInfo ri = new ResolveInfo();
                ri.activityInfo = ai;
                list.add(ri);
                return list;
            }
        }

        
        synchronized (mPackages) {
            return mComponentResolver.queryActivities(intent, resolvedType, flags, userId);
        }
    }

    @Override
    public List<ResolveInfo> queryIntentServices(
            Intent intent, int flags, int userId) {
        final String resolvedType = intent.resolveTypeIfNeeded(PrismSpaceCore.getContext().getContentResolver());
        return this.queryIntentServicesInternal(intent, resolvedType, flags, userId);
    }

    private ActivityInfo getActivity(ComponentName component, int flags,
                                     int userId) {
        flags = updateFlags(flags, userId);
        synchronized (mPackages) {
            PPackage.Activity a = mComponentResolver.getActivity(component);

            if (a != null) {
                PPackageSettings ps = mSettings.mPackages.get(component.getPackageName());
                if (ps == null) return null;
                return PackageManagerCompat.generateActivityInfo(a, flags, ps.readUserState(userId), userId);
            }
        }
        return null;
    }

    @Override
    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        if (!sUserManager.exists(userId)) return null;
        if (Objects.equals(packageName, PrismSpaceCore.getHostPkg())) {
            try {
                return PrismSpaceCore.getPackageManager().getPackageInfo(packageName, flags);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        flags = updateFlags(flags, userId);
        PPackageSettings ps = null;
        
        synchronized (mPackages) {
            
            ps = mPackages.get(packageName);
        }
        if (ps != null) {
            return PackageManagerCompat.generatePackageInfo(ps, flags, ps.readUserState(userId), userId);
        }
        return null;
    }

    @Override
    public ServiceInfo getServiceInfo(ComponentName component, int flags, int userId) {
        if (!sUserManager.exists(userId)) return null;
        synchronized (mPackages) {
            PPackage.Service s = mComponentResolver.getService(component);
            if (s != null) {
                PPackageSettings ps = mPackages.get(component.getPackageName());
                if (ps == null) return null;
                return PackageManagerCompat.generateServiceInfo(
                        s, flags, ps.readUserState(userId), userId);
            }
        }
        return null;
    }

    @Override
    public ActivityInfo getReceiverInfo(ComponentName component, int flags, int userId) {
        if (!sUserManager.exists(userId)) return null;
        synchronized (mPackages) {
            PPackage.Activity a = mComponentResolver.getReceiver(component);
            if (a != null) {
                PPackageSettings ps = mPackages.get(component.getPackageName());
                if (ps == null) return null;
                return PackageManagerCompat.generateActivityInfo(
                        a, flags, ps.readUserState(userId), userId);
            }
        }
        return null;
    }

    @Override
    public ActivityInfo getActivityInfo(ComponentName component, int flags, int userId) {
        if (!sUserManager.exists(userId)) return null;
        synchronized (mPackages) {
            PPackage.Activity a = mComponentResolver.getActivity(component);

            if (a != null) {
                PPackageSettings ps = mPackages.get(component.getPackageName());
                if (ps == null) return null;
                return PackageManagerCompat.generateActivityInfo(
                        a, flags, ps.readUserState(userId), userId);
            }
        }
        return null;
    }

    @Override
    public ProviderInfo getProviderInfo(ComponentName component, int flags, int userId) {
        if (!sUserManager.exists(userId)) return null;
        synchronized (mPackages) {
            PPackage.Provider p = mComponentResolver.getProvider(component);
            if (p != null) {
                PPackageSettings ps = mPackages.get(component.getPackageName());
                if (ps == null) return null;
                return PackageManagerCompat.generateProviderInfo(
                        p, flags, ps.readUserState(userId), userId);
            }
        }
        return null;
    }

    @Override
    public List<ApplicationInfo> getInstalledApplications(int flags, int userId) {
        return getInstalledApplicationsListInternal(flags, userId, Binder.getCallingUid());
    }

    @Override
    public List<PackageInfo> getInstalledPackages(int flags, int userId) {
        final int callingUid = Binder.getCallingUid();



        if (!sUserManager.exists(userId)) return Collections.emptyList();

        
        synchronized (mPackages) {
            ArrayList<PackageInfo> list;
            list = new ArrayList<>(mPackages.size());
            for (PPackageSettings ps : mPackages.values()) {






                PackageInfo pi = getPackageInfo(ps.pkg.packageName, flags, userId);
                if (pi != null) {
                    list.add(pi);
                }
            }
            return new ArrayList<>(list);
        }
    }

    private List<ApplicationInfo> getInstalledApplicationsListInternal(int flags, int userId,
                                                                       int callingUid) {
        if (!sUserManager.exists(userId)) return Collections.emptyList();

        
        synchronized (mPackages) {
            ArrayList<ApplicationInfo> list;
            list = new ArrayList<>(mPackages.size());
            Collection<PPackageSettings> packageSettings = mPackages.values();
            for (PPackageSettings ps : packageSettings) {








                ApplicationInfo ai = PackageManagerCompat.generateApplicationInfo(ps.pkg, flags,
                        ps.readUserState(userId), userId);
                if (ai != null) {
                    list.add(ai);
                }
            }
            return list;
        }
    }

    @Override
    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags, String resolvedType, int userId) throws RemoteException {
        if (!sUserManager.exists(userId)) return Collections.emptyList();
        final String pkgName = intent.getPackage();
        ComponentName comp = intent.getComponent();
        if (comp == null) {
            if (intent.getSelector() != null) {
                intent = intent.getSelector();
                comp = intent.getComponent();
            }
        }

        if (comp != null) {
            final List<ResolveInfo> list = new ArrayList<>(1);
            final ActivityInfo ai = getActivityInfo(comp, flags, userId);
            if (ai != null) {
                
                
                
                
                final ResolveInfo ri = new ResolveInfo();
                ri.activityInfo = ai;
                list.add(ri);
            }
            return list;
        }

        
        List<ResolveInfo> result;
        synchronized (mPackages) {
            if (pkgName != null) {
                PPackageSettings bPackageSettings = mPackages.get(pkgName);
                result = null;
                if (bPackageSettings != null) {
                    final PPackage pkg = bPackageSettings.pkg;

                    result = mComponentResolver.queryActivities(
                            intent, resolvedType, flags, pkg.activities, userId);
                }
                if (result == null || result.size() == 0) {
                    
                    
                    if (result == null) {
                        result = new ArrayList<>();
                    }
                }
                return result;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags, String resolvedType, int userId) throws RemoteException {
        if (!sUserManager.exists(userId)) return Collections.emptyList();

        ComponentName comp = intent.getComponent();
        if (comp == null) {
            if (intent.getSelector() != null) {
                intent = intent.getSelector();
                comp = intent.getComponent();
            }
        }
        if (comp != null) {
            final List<ResolveInfo> list = new ArrayList<>(1);
            final ActivityInfo ai = getReceiverInfo(comp, flags, userId);
            if (ai != null) {
                
                
                
                
                ResolveInfo ri = new ResolveInfo();
                ri.activityInfo = ai;
                list.add(ri);
            }
            return list;
        }

        
        synchronized (mPackages) {
            String pkgName = intent.getPackage();
            PPackageSettings bPackageSettings = mPackages.get(pkgName);
            if (bPackageSettings != null) {
                final PPackage pkg = bPackageSettings.pkg;
                return mComponentResolver.queryReceivers(
                        intent, resolvedType, flags, pkg.receivers, userId);
            } else {
                return mComponentResolver.queryReceivers(intent, resolvedType, flags, userId);
            }
        }
    }

    @Override
    public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags, int userId) throws RemoteException {
        if (!sUserManager.exists(userId)) return Collections.emptyList();

        List<ProviderInfo> providers = new ArrayList<>();
        if (TextUtils.isEmpty(processName))
            return providers;
        providers.addAll(mComponentResolver.queryProviders(processName, null, flags, userId));
        return providers;
    }

    @Override
    public InstallResult installPackageAsUser(String file, InstallOption option, int userId) {
        synchronized (mInstallLock) {
            return installPackageAsUserLocked(file, option, userId);
        }
    }

    @Override
    public void uninstallPackageAsUser(String packageName, int userId) throws RemoteException {
        synchronized (mInstallLock) {
            synchronized (mPackages) {
                PPackageSettings ps = mPackages.get(packageName);
                if (ps == null)
                    return;

                if (!isInstalled(packageName, userId)) {
                    return;
                }
                boolean removeApp = ps.getUserState().size() <= 1;
                PProcessManagerService.get().killPackageAsUser(packageName, userId);
                int i = PPackageInstallerService.get().uninstallPackageAsUser(ps, removeApp, userId);
                if (i < 0) {
                    
                }

                if (removeApp) {
                    mSettings.removePackage(packageName);
                    mComponentResolver.removeAllComponents(ps.pkg);
                } else {
                    ps.removeUser(userId);
                    ps.save();
                }
                onPackageUninstalled(packageName, removeApp, userId);
            }
        }
    }

    @Override
    public void uninstallPackage(String packageName) {
        synchronized (mInstallLock) {
            synchronized (mPackages) {
                PPackageSettings ps = mPackages.get(packageName);
                if (ps == null)
                    return;
                PProcessManagerService.get().killAllByPackageName(packageName);
                    for (Integer userId : ps.getUserIds()) {
                        int i = PPackageInstallerService.get().uninstallPackageAsUser(ps, true, userId);
                        if (i < 0) {
                            continue;
                        }
                        onPackageUninstalled(packageName, true, userId);
                    }
                mSettings.removePackage(packageName);
                mComponentResolver.removeAllComponents(ps.pkg);
            }
        }
    }

    @Override
    public void clearPackage(String packageName, int userId) {
        if (!isInstalled(packageName, userId)) {
            return;
        }
        PProcessManagerService.get().killPackageAsUser(packageName, userId);
        PPackageSettings ps = mPackages.get(packageName);
        if (ps == null)
            return;
        int i = PPackageInstallerService.get().clearPackage(ps, userId);
    }

    @Override
    public void stopPackage(String packageName, int userId) {
        PActivityManagerService.get().forceStopPackage(packageName, userId);
        PProcessManagerService.get().killPackageAsUser(packageName, userId);
    }

    @Override
    public void deleteUser(int userId) throws RemoteException {
        synchronized (mPackages) {
            for (PPackageSettings ps : mPackages.values()) {
                uninstallPackageAsUser(ps.pkg.packageName, userId);
            }
        }
    }

    @Override
    public boolean isInstalled(String packageName, int userId) {
        if (!sUserManager.exists(userId)) return false;
        synchronized (mPackages) {
            PPackageSettings ps = mPackages.get(packageName);
            if (ps == null)
                return false;
            return ps.getInstalled(userId);
        }
    }

    @Override
    public List<InstalledPackage> getInstalledPackagesAsUser(int userId) {
        if (!sUserManager.exists(userId)) return Collections.emptyList();
        synchronized (mPackages) {
            List<InstalledPackage> installedPackages = new ArrayList<>();
            for (PPackageSettings ps : mPackages.values()) {
                if (ps.getInstalled(userId) && !GmsCore.isGoogleAppOrService(ps.pkg.packageName)) {
                    InstalledPackage installedPackage = new InstalledPackage();
                    installedPackage.userId = userId;
                    installedPackage.packageName = ps.pkg.packageName;
                    installedPackages.add(installedPackage);
                }
            }
            return installedPackages;
        }
    }

    @Override
    public String[] getPackagesForUid(int uid, int userId) throws RemoteException {
        if (!sUserManager.exists(userId)) return new String[]{};
        synchronized (mPackages) {
            List<String> packages = new ArrayList<>();
            for (PPackageSettings ps : mPackages.values()) {
                String packageName = ps.pkg.packageName;
                if (ps.getInstalled(userId) && getAppId(packageName) == uid) {
                    packages.add(packageName);
                }
            }
            if (packages.isEmpty()) {
                ProcessRecord processByPid = PProcessManagerService.get().findProcessByPid(getCallingPid());
                if (processByPid != null) {
                    packages.add(processByPid.getPackageName());
                }
            }
            return packages.toArray(new String[]{});
        }
    }

    private InstallResult installPackageAsUserLocked(String file, InstallOption option, int userId) {
        long l = System.currentTimeMillis();
        InstallResult result = new InstallResult();
        File apkFile = null;
        try {
            if (!sUserManager.exists(userId)) {
                sUserManager.createUser(userId);
            }
            if (option.isFlag(InstallOption.FLAG_URI_FILE)) {
                apkFile = new File(PEnvironment.getCacheDir(), UUID.randomUUID().toString() + ".apk");
                InputStream inputStream = PrismSpaceCore.getContext().getContentResolver().openInputStream(Uri.parse(file));
                FileUtils.copyFile(inputStream, apkFile);
            } else {
                apkFile = new File(file);
            }



            PackageInfo packageArchiveInfo = PrismSpaceCore.getPackageManager().getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
            if (packageArchiveInfo == null) {
                return result.installError("getPackageArchiveInfo error.Please check whether APK is normal.");
            }

            
            String packageName = packageArchiveInfo.packageName;
            String hostPackageName = PrismSpaceCore.getHostPkg();
            if (packageName.equals(hostPackageName)) {
                return result.installError("Cannot clone PrismSpace app from within PrismSpace. This would create infinite recursion and is not allowed for security reasons.");
            }
            
            
            if (packageName.contains("prismspace") ||
                packageName.contains("vspace") || packageName.contains("virtual")) {
                
                Slog.w(TAG, "Installing potentially PrismSpace-related app: " + packageName + ". Proceed with caution.");
            }

            boolean support = AbiUtils.isSupport(apkFile);
            if (!support) {
                String msg = packageArchiveInfo.applicationInfo.loadLabel(PrismSpaceCore.getPackageManager()) + "[" + packageArchiveInfo.packageName + "]";
                return result.installError(packageArchiveInfo.packageName,
                        msg + "\n" + (PrismSpaceCore.is64Bit() ? "The box does not support 32-bit Application" : "The box does not support 64-bit Application"));
            }
            PackageParser.Package aPackage = parserApk(apkFile.getAbsolutePath());
            if (aPackage == null) {
                return result.installError("parser apk error.");
            }
            result.packageName = aPackage.packageName;

            if (option.isFlag(InstallOption.FLAG_SYSTEM)) {
                aPackage.applicationInfo = PrismSpaceCore.getPackageManager().getPackageInfo(aPackage.packageName, 0).applicationInfo;
            }
            PPackageSettings bPackageSettings = mSettings.getPackageLPw(aPackage.packageName, aPackage, option);

            
            PProcessManagerService.get().killPackageAsUser(aPackage.packageName, userId);

            int i = PPackageInstallerService.get().installPackageAsUser(bPackageSettings, userId);
            if (i < 0) {
                return result.installError("install apk error.");
            }
            synchronized (mPackages) {
                bPackageSettings.setInstalled(true, userId);
                bPackageSettings.save();
            }
            mComponentResolver.removeAllComponents(bPackageSettings.pkg);
            mComponentResolver.addAllComponents(bPackageSettings.pkg);
            mSettings.scanPackage(aPackage.packageName);
            onPackageInstalled(bPackageSettings.pkg.packageName, userId);
            return result;
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (apkFile != null && option.isFlag(InstallOption.FLAG_URI_FILE)) {
                FileUtils.deleteDir(apkFile);
            }
            Slog.d(TAG, "install finish: " + (System.currentTimeMillis() - l) + "ms");
        }
        return result;
    }

    private PackageParser.Package parserApk(String file) {
        try {
            PackageParser parser = PackageParserCompat.createParser(new File(file));
            PackageParser.Package aPackage = PackageParserCompat.parsePackage(parser, new File(file), 0);
            PackageParserCompat.collectCertificates(parser, aPackage, 0);
            return aPackage;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    static String fixProcessName(String defProcessName, String processName) {
        if (processName == null) {
            return defProcessName;
        }
        return processName;
    }

    
    private int updateFlags(int flags, int userId) {
        if ((flags & (PackageManager.MATCH_DIRECT_BOOT_UNAWARE
                | PackageManager.MATCH_DIRECT_BOOT_AWARE)) != 0) {
            
            
            
        } else {
            
            flags |= PackageManager.MATCH_DIRECT_BOOT_AWARE | MATCH_DIRECT_BOOT_UNAWARE;
        }
        return flags;
    }

    public int getAppId(String packageName) {
        PPackageSettings bPackageSettings = mPackages.get(packageName);
        if (bPackageSettings != null)
            return bPackageSettings.appId;
        return -1;
    }

    Settings getSettings() {
        return mSettings;
    }

    public void addPackageMonitor(PackageMonitor monitor) {
        mPackageMonitors.add(monitor);
    }

    public void removePackageMonitor(PackageMonitor monitor) {
        mPackageMonitors.remove(monitor);
    }

    void onPackageUninstalled(String packageName, boolean isRemove, int userId) {
        for (PackageMonitor packageMonitor : mPackageMonitors) {
            packageMonitor.onPackageUninstalled(packageName, isRemove, userId);
        }
        Slog.d(TAG, "onPackageUninstalled: " + packageName + ", userId: " + userId);
    }

    void onPackageInstalled(String packageName, int userId) {
        for (PackageMonitor packageMonitor : mPackageMonitors) {
            packageMonitor.onPackageInstalled(packageName, userId);
        }
        Slog.d(TAG, "onPackageInstalled: " + packageName + ", userId: " + userId);
    }

    public PPackageSettings getBPackageSetting(String packageName) {
        return mPackages.get(packageName);
    }

    public List<PPackageSettings> getBPackageSettings() {
        return new ArrayList<>(mPackages.values());
    }

    @Override
    public void systemReady() {
        mSettings.scanPackage();
        for (PPackageSettings value : mPackages.values()) {
            mComponentResolver.removeAllComponents(value.pkg);
            mComponentResolver.addAllComponents(value.pkg);
        }
    }
}
