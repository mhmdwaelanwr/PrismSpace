package com.prismspace.container.core.system.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.util.ArrayMap;

import java.util.ArrayList;
import java.util.List;

import com.prismspace.container.utils.Slog;



public class ComponentResolver {
    public static final String TAG = "ComponentResolver";

    private final Object mLock = new Object();

    
    private final ActivityIntentResolver mActivities = new ActivityIntentResolver();

    
    private final ProviderIntentResolver mProviders = new ProviderIntentResolver();

    
    private final ActivityIntentResolver mReceivers = new ActivityIntentResolver();

    
    private final ServiceIntentResolver mServices = new ServiceIntentResolver();
    
    private final ArrayMap<String, PPackage.Provider> mProvidersByAuthority = new ArrayMap<>();

    public ComponentResolver() {
    }

    void addAllComponents(PPackage pkg) {
        final ArrayList<PPackage.ActivityIntentInfo> newIntents = new ArrayList<>();
        synchronized (mLock) {
            addActivitiesLocked(pkg, newIntents);
            addServicesLocked(pkg);
            addProvidersLocked(pkg);
            addReceiversLocked(pkg);
        }
    }

    void removeAllComponents(PPackage pkg) {
        synchronized (mLock) {
            removeAllComponentsLocked(pkg);
        }
    }

    private void removeAllComponentsLocked(PPackage pkg) {
        int componentSize;
        StringBuilder r;
        int i;

        componentSize = pkg.activities.size();
        r = null;
        for (i = 0; i < componentSize; i++) {
            PPackage.Activity a = pkg.activities.get(i);
            mActivities.removeActivity(a, "activity");
        }
        componentSize = pkg.providers.size();
        r = null;
        for (i = 0; i < componentSize; i++) {
            PPackage.Provider p = pkg.providers.get(i);
            mProviders.removeProvider(p);
            if (p.info.authority == null) {
                
                
                
                continue;
            }
            String[] names = p.info.authority.split(";");
            for (int j = 0; j < names.length; j++) {
                if (mProvidersByAuthority.get(names[j]) == p) {
                    mProvidersByAuthority.remove(names[j]);
                }
            }
            mProvidersByAuthority.remove(p.info.authority);
        }

        componentSize = pkg.receivers.size();
        r = null;
        for (i = 0; i < componentSize; i++) {
            PPackage.Activity a = pkg.receivers.get(i);
            mReceivers.removeActivity(a, "receiver");
        }

        componentSize = pkg.services.size();
        r = null;
        for (i = 0; i < componentSize; i++) {
            PPackage.Service s = pkg.services.get(i);
            mServices.removeService(s);
        }
    }

    private void addActivitiesLocked(PPackage pkg,
                                     List<PPackage.ActivityIntentInfo> newIntents) {
        final int activitiesSize = pkg.activities.size();
        for (int i = 0; i < activitiesSize; i++) {
            PPackage.Activity a = pkg.activities.get(i);
            a.info.processName =
                    PPackageManagerService.fixProcessName(pkg.applicationInfo.processName, a.info.processName);
            mActivities.addActivity(a, "activity", newIntents);
        }
    }

    private void addProvidersLocked(PPackage pkg) {
        final int providersSize = pkg.providers.size();
        for (int i = 0; i < providersSize; i++) {
            PPackage.Provider p = pkg.providers.get(i);
            p.info.processName = PPackageManagerService.fixProcessName(pkg.applicationInfo.processName,
                    p.info.processName);
            mProviders.addProvider(p);
            if (p.info.authority != null) {
                String[] names = p.info.authority.split(";");
                p.info.authority = null;
                for (String name : names) {
                    if (!mProvidersByAuthority.containsKey(name)) {
                        mProvidersByAuthority.put(name, p);
                        if (p.info.authority == null) {
                            p.info.authority = name;
                        } else {
                            p.info.authority = p.info.authority + ";" + name;
                        }
                    } else {
                        final PPackage.Provider other =
                                mProvidersByAuthority.get(name);
                        final ComponentName component =
                                (other != null && other.getComponentName() != null)
                                        ? other.getComponentName() : null;
                        final String packageName =
                                component != null ? component.getPackageName() : "?";
                        Slog.w(TAG, "Skipping provider name " + name
                                + " (in package " + pkg.applicationInfo.packageName + ")"
                                + ": name already used by " + packageName);
                    }
                }
            }
        }
    }

    private void addReceiversLocked(PPackage pkg) {
        final int receiversSize = pkg.receivers.size();
        for (int i = 0; i < receiversSize; i++) {
            PPackage.Activity a = pkg.receivers.get(i);
            a.info.processName = PPackageManagerService.fixProcessName(pkg.applicationInfo.processName,
                    a.info.processName);
            mReceivers.addActivity(a, "receiver", null);
        }
    }

    private void addServicesLocked(PPackage pkg) {
        final int servicesSize = pkg.services.size();
        for (int i = 0; i < servicesSize; i++) {
            PPackage.Service s = pkg.services.get(i);
            s.info.processName = PPackageManagerService.fixProcessName(pkg.applicationInfo.processName,
                    s.info.processName);
            mServices.addService(s);
        }
    }


    
    PPackage.Activity getActivity(ComponentName component) {
        synchronized (mLock) {
            return mActivities.mActivities.get(component);
        }
    }

    
    PPackage.Provider getProvider(ComponentName component) {
        synchronized (mLock) {
            return mProviders.mProviders.get(component);
        }
    }

    
    PPackage.Activity getReceiver(ComponentName component) {
        synchronized (mLock) {
            return mReceivers.mActivities.get(component);
        }
    }

    
    PPackage.Service getService(ComponentName component) {
        synchronized (mLock) {
            return mServices.mServices.get(component);
        }
    }

    List<ResolveInfo> queryActivities(Intent intent, String resolvedType, int flags, int userId) {
        synchronized (mLock) {
            return mActivities.queryIntent(intent, resolvedType, flags, userId);
        }
    }

    List<ResolveInfo> queryActivities(Intent intent, String resolvedType, int flags,
                                      List<PPackage.Activity> activities, int userId) {
        synchronized (mLock) {
            return mActivities.queryIntentForPackage(
                    intent, resolvedType, flags, activities, userId);
        }
    }

    List<ResolveInfo> queryProviders(Intent intent, String resolvedType, int flags, int userId) {
        synchronized (mLock) {
            return mProviders.queryIntent(intent, resolvedType, flags, userId);
        }
    }

    List<ResolveInfo> queryProviders(Intent intent, String resolvedType, int flags,
                                     List<PPackage.Provider> providers, int userId) {
        synchronized (mLock) {
            return mProviders.queryIntentForPackage(intent, resolvedType, flags, providers, userId);
        }
    }

    List<ProviderInfo> queryProviders(String processName, String metaDataKey, int flags,
                                      int userId) {
        List<ProviderInfo> providerList = new ArrayList<>();
        synchronized (mLock) {
            for (int i = mProviders.mProviders.size() - 1; i >= 0; --i) {
                final PPackage.Provider p = mProviders.mProviders.valueAt(i);
                final PPackageSettings ps = (PPackageSettings) p.owner.mExtras;
                if (ps == null) {
                    continue;
                }
                if (p.info.authority == null) {
                    continue;
                }
                if (processName != null && (!p.info.processName.equals(processName))) {
                    continue;
                }
                
                if (metaDataKey != null
                        && (p.metaData == null || !p.metaData.containsKey(metaDataKey))) {
                    continue;
                }
                final ProviderInfo info = PackageManagerCompat.generateProviderInfo(p, flags, ps.readUserState(userId), userId);
                if (info == null) {
                    continue;
                }



                providerList.add(info);
            }
        }
        return providerList;
    }

    ProviderInfo queryProvider(String authority, int flags, int userId) {
        synchronized (mLock) {
            final PPackage.Provider p = mProvidersByAuthority.get(authority);
            if (p == null) {
                return null;
            }
            PPackageSettings ps = p.owner.mExtras;
            return PackageManagerCompat.generateProviderInfo(p, flags, ps.readUserState(userId), userId);
        }
    }

    List<ResolveInfo> queryReceivers(Intent intent, String resolvedType, int flags, int userId) {
        synchronized (mLock) {
            return mReceivers.queryIntent(intent, resolvedType, flags, userId);
        }
    }

    List<ResolveInfo> queryReceivers(Intent intent, String resolvedType, int flags,
                                     List<PPackage.Activity> receivers, int userId) {
        synchronized (mLock) {
            return mReceivers.queryIntentForPackage(intent, resolvedType, flags, receivers, userId);
        }
    }

    List<ResolveInfo> queryServices(Intent intent, String resolvedType, int flags, int userId) {
        synchronized (mLock) {
            return mServices.queryIntent(intent, resolvedType, flags, userId);
        }
    }

    List<ResolveInfo> queryServices(Intent intent, String resolvedType, int flags,
                                    List<PPackage.Service> services, int userId) {
        synchronized (mLock) {
            return mServices.queryIntentForPackage(intent, resolvedType, flags, services, userId);
        }
    }


    private static final class ServiceIntentResolver extends IntentResolver<PPackage.ServiceIntentInfo, ResolveInfo> {

        @Override
        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType,
                                             boolean defaultOnly, int userId) {
            mFlags = defaultOnly ? PackageManager.MATCH_DEFAULT_ONLY : 0;
            return super.queryIntent(intent, resolvedType, defaultOnly, userId);
        }

        List<ResolveInfo> queryIntent(Intent intent, String resolvedType, int flags,
                                      int userId) {
            mFlags = flags;
            return super.queryIntent(intent, resolvedType,
                    (flags & PackageManager.MATCH_DEFAULT_ONLY) != 0,
                    userId);
        }

        List<ResolveInfo> queryIntentForPackage(Intent intent, String resolvedType,
                                                int flags, List<PPackage.Service> packageServices, int userId) {
            if (packageServices == null) {
                return null;
            }
            mFlags = flags;
            final boolean defaultOnly = (flags & PackageManager.MATCH_DEFAULT_ONLY) != 0;
            final int servicesSize = packageServices.size();
            ArrayList<PPackage.ServiceIntentInfo[]> listCut = new ArrayList<>(servicesSize);

            ArrayList<PPackage.ServiceIntentInfo> intentFilters;
            for (int i = 0; i < servicesSize; ++i) {
                intentFilters = packageServices.get(i).intents;
                if (intentFilters != null && intentFilters.size() > 0) {
                    PPackage.ServiceIntentInfo[] array =
                            new PPackage.ServiceIntentInfo[intentFilters.size()];
                    intentFilters.toArray(array);
                    listCut.add(array);
                }
            }
            return super.queryIntentFromList(intent, resolvedType, defaultOnly, listCut, userId);
        }

        void addService(PPackage.Service s) {
            mServices.put(s.getComponentName(), s);
            final int intentsSize = s.intents.size();
            int j;
            for (j = 0; j < intentsSize; j++) {
                PPackage.ServiceIntentInfo intent = s.intents.get(j);
                addFilter(intent);
            }
        }

        void removeService(PPackage.Service s) {
            mServices.remove(s.getComponentName());
            final int intentsSize = s.intents.size();
            int j;
            for (j = 0; j < intentsSize; j++) {
                PPackage.ServiceIntentInfo intent = s.intents.get(j);
                removeFilter(intent);
            }
        }

        @Override
        protected boolean isPackageForFilter(String packageName,
                                             PPackage.ServiceIntentInfo info) {
            return packageName.equals(info.service.owner.packageName);
        }

        @Override
        protected PPackage.ServiceIntentInfo[] newArray(int size) {
            return new PPackage.ServiceIntentInfo[size];
        }

        @Override
        protected ResolveInfo newResult(PPackage.ServiceIntentInfo filter, int match, int userId) {
            final PPackage.ServiceIntentInfo info = (PPackage.ServiceIntentInfo) filter;
            final PPackage.Service service = info.service;
            PPackageSettings ps = (PPackageSettings) service.owner.mExtras;
            if (ps == null) {
                return null;
            }
            ServiceInfo si = PackageManagerCompat.generateServiceInfo(service, mFlags, ps.readUserState(userId), userId);

            final ResolveInfo res = new ResolveInfo();
            res.serviceInfo = si;
            if ((mFlags & PackageManager.GET_RESOLVED_FILTER) != 0) {
                res.filter = filter.intentFilter;
            }
            res.priority = info.intentFilter.getPriority();
            res.preferredOrder = service.owner.mPreferredOrder;
            res.match = match;
            res.isDefault = info.hasDefault;
            res.labelRes = info.labelRes;
            res.nonLocalizedLabel = info.nonLocalizedLabel;
            res.icon = info.icon;
            return res;
        }

        
        private final ArrayMap<ComponentName, PPackage.Service> mServices = new ArrayMap<>();
        private int mFlags;
    }


    private static final class ActivityIntentResolver extends IntentResolver<PPackage.ActivityIntentInfo, ResolveInfo> {

        @Override
        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType,
                                             boolean defaultOnly, int userId) {
            mFlags = (defaultOnly ? PackageManager.MATCH_DEFAULT_ONLY : 0);
            return super.queryIntent(intent, resolvedType, defaultOnly, userId);
        }

        List<ResolveInfo> queryIntent(Intent intent, String resolvedType, int flags,
                                      int userId) {
            mFlags = flags;
            return super.queryIntent(intent, resolvedType,
                    (flags & PackageManager.MATCH_DEFAULT_ONLY) != 0,
                    userId);
        }

        List<ResolveInfo> queryIntentForPackage(Intent intent, String resolvedType,
                                                int flags, List<PPackage.Activity> packageActivities, int userId) {
            if (packageActivities == null) {
                return null;
            }
            mFlags = flags;
            final boolean defaultOnly = (flags & PackageManager.MATCH_DEFAULT_ONLY) != 0;
            final int activitiesSize = packageActivities.size();
            ArrayList<PPackage.ActivityIntentInfo[]> listCut = new ArrayList<>(activitiesSize);

            ArrayList<PPackage.ActivityIntentInfo> intentFilters;
            for (int i = 0; i < activitiesSize; ++i) {
                intentFilters = packageActivities.get(i).intents;
                if (intentFilters != null && intentFilters.size() > 0) {
                    PPackage.ActivityIntentInfo[] array =
                            new PPackage.ActivityIntentInfo[intentFilters.size()];
                    intentFilters.toArray(array);
                    listCut.add(array);
                }
            }
            return super.queryIntentFromList(intent, resolvedType, defaultOnly, listCut, userId);
        }

        private void addActivity(PPackage.Activity a, String type,
                                 List<PPackage.ActivityIntentInfo> newIntents) {
            mActivities.put(a.getComponentName(), a);
            final int intentsSize = a.intents.size();
            for (int j = 0; j < intentsSize; j++) {
                PPackage.ActivityIntentInfo intent = a.intents.get(j);
                if (newIntents != null && "activity".equals(type)) {
                    newIntents.add(intent);
                }
                addFilter(intent);
            }
        }

        private void removeActivity(PPackage.Activity a, String type) {
            mActivities.remove(a.getComponentName());
            final int intentsSize = a.intents.size();
            for (int j = 0; j < intentsSize; j++) {
                PPackage.ActivityIntentInfo intent = a.intents.get(j);
                removeFilter(intent);
            }
        }

        @Override
        protected boolean isPackageForFilter(String packageName,
                                             PPackage.ActivityIntentInfo info) {
            return packageName.equals(info.activity.owner.packageName);
        }

        @Override
        protected PPackage.ActivityIntentInfo[] newArray(int size) {
            return new PPackage.ActivityIntentInfo[size];
        }

        @Override
        protected ResolveInfo newResult(PPackage.ActivityIntentInfo info, int match, int userId) {
            final PPackage.Activity activity = info.activity;
            PPackageSettings ps = (PPackageSettings) activity.owner.mExtras;
            if (ps == null) {
                return null;
            }
            ActivityInfo ai =
                    PackageManagerCompat.generateActivityInfo(activity, mFlags, ps.readUserState(userId), userId);

            final ResolveInfo res = new ResolveInfo();
            res.activityInfo = ai;
            if ((mFlags & PackageManager.GET_RESOLVED_FILTER) != 0) {
                res.filter = info.intentFilter;
            }
            res.priority = info.intentFilter.getPriority();
            res.preferredOrder = activity.owner.mPreferredOrder;
            
            
            res.match = match;
            res.isDefault = info.hasDefault;
            res.labelRes = info.labelRes;
            res.nonLocalizedLabel = info.nonLocalizedLabel;
            res.icon = info.icon;
            return res;
        }

        
        private final ArrayMap<ComponentName, PPackage.Activity> mActivities =
                new ArrayMap<>();
        private int mFlags;
    }

    private static final class ProviderIntentResolver
            extends IntentResolver<PPackage.ProviderIntentInfo, ResolveInfo> {
        @Override
        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType,
                                             boolean defaultOnly, int userId) {
            mFlags = defaultOnly ? PackageManager.MATCH_DEFAULT_ONLY : 0;
            return super.queryIntent(intent, resolvedType, defaultOnly, userId);
        }

        List<ResolveInfo> queryIntent(Intent intent, String resolvedType, int flags,
                                      int userId) {
            mFlags = flags;
            return super.queryIntent(intent, resolvedType,
                    (flags & PackageManager.MATCH_DEFAULT_ONLY) != 0,
                    userId);
        }

        List<ResolveInfo> queryIntentForPackage(Intent intent, String resolvedType,
                                                int flags, List<PPackage.Provider> packageProviders, int userId) {
            if (packageProviders == null) {
                return null;
            }
            mFlags = flags;
            final boolean defaultOnly = (flags & PackageManager.MATCH_DEFAULT_ONLY) != 0;
            final int providersSize = packageProviders.size();
            ArrayList<PPackage.ProviderIntentInfo[]> listCut = new ArrayList<>(providersSize);

            ArrayList<PPackage.ProviderIntentInfo> intentFilters;
            for (int i = 0; i < providersSize; ++i) {
                intentFilters = packageProviders.get(i).intents;
                if (intentFilters != null && intentFilters.size() > 0) {
                    PPackage.ProviderIntentInfo[] array =
                            new PPackage.ProviderIntentInfo[intentFilters.size()];
                    intentFilters.toArray(array);
                    listCut.add(array);
                }
            }
            return super.queryIntentFromList(intent, resolvedType, defaultOnly, listCut, userId);
        }

        void addProvider(PPackage.Provider p) {
            mProviders.put(p.getComponentName(), p);
            final int intentsSize = p.intents.size();
            int j;
            for (j = 0; j < intentsSize; j++) {
                PPackage.ProviderIntentInfo intent = p.intents.get(j);
                addFilter(intent);
            }
        }

        void removeProvider(PPackage.Provider p) {
            mProviders.remove(p.getComponentName());
            final int intentsSize = p.intents.size();
            int j;
            for (j = 0; j < intentsSize; j++) {
                PPackage.ProviderIntentInfo intent = p.intents.get(j);
                removeFilter(intent);
            }
        }

        @Override
        protected boolean allowFilterResult(
                PPackage.ProviderIntentInfo filter, List<ResolveInfo> dest) {
            ProviderInfo filterPi = filter.provider.info;
            for (int i = dest.size() - 1; i >= 0; i--) {
                ProviderInfo destPi = dest.get(i).providerInfo;
                if (destPi.name.equals(filterPi.name)
                        && destPi.packageName.equals(filterPi.packageName)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected PPackage.ProviderIntentInfo[] newArray(int size) {
            return new PPackage.ProviderIntentInfo[size];
        }

        @Override
        protected boolean isPackageForFilter(String packageName,
                                             PPackage.ProviderIntentInfo info) {
            return packageName.equals(info.provider.owner.packageName);
        }

        @Override
        protected ResolveInfo newResult(PPackage.ProviderIntentInfo filter, int match, int userId) {
            final PPackage.ProviderIntentInfo info = filter;
            final PPackage.Provider provider = info.provider;
            PPackageSettings ps = (PPackageSettings) provider.owner.mExtras;
            if (ps == null) {
                return null;
            }

            ProviderInfo pi = PackageManagerCompat.generateProviderInfo(provider, mFlags, ps.readUserState(userId), userId);
            final ResolveInfo res = new ResolveInfo();
            res.providerInfo = pi;
            if ((mFlags & PackageManager.GET_RESOLVED_FILTER) != 0) {
                res.filter = filter.intentFilter;
            }
            res.priority = info.intentFilter.getPriority();
            res.preferredOrder = provider.owner.mPreferredOrder;
            res.match = match;
            res.isDefault = info.hasDefault;
            res.labelRes = info.labelRes;
            res.nonLocalizedLabel = info.nonLocalizedLabel;
            res.icon = info.icon;
            return res;
        }

        private final ArrayMap<ComponentName, PPackage.Provider> mProviders = new ArrayMap<>();
        private int mFlags;
    }
}

