package com.prismspace.container.core.system;

import android.os.IBinder;

import java.util.HashMap;
import java.util.Map;

import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.core.system.accounts.PAccountManagerService;
import com.prismspace.container.core.system.am.PActivityManagerService;
import com.prismspace.container.core.system.am.PJobManagerService;
import com.prismspace.container.core.system.location.PLocationManagerService;
import com.prismspace.container.core.system.notification.PNotificationManagerService;
import com.prismspace.container.core.system.os.PStorageManagerService;
import com.prismspace.container.core.system.pm.PPackageManagerService;

import com.prismspace.container.core.system.user.PUserManagerService;


public class ServiceManager {
    private static ServiceManager sServiceManager = null;
    public static final String ACTIVITY_MANAGER = "activity_manager";
    public static final String JOB_MANAGER = "job_manager";
    public static final String PACKAGE_MANAGER = "package_manager";
    public static final String STORAGE_MANAGER = "storage_manager";
    public static final String USER_MANAGER = "user_manager";

    public static final String ACCOUNT_MANAGER = "account_manager";
    public static final String LOCATION_MANAGER = "location_manager";
    public static final String NOTIFICATION_MANAGER = "notification_manager";

    private final Map<String, IBinder> mCaches = new HashMap<>();

    public static ServiceManager get() {
        if (sServiceManager == null) {
            synchronized (ServiceManager.class) {
                if (sServiceManager == null) {
                    sServiceManager = new ServiceManager();
                }
            }
        }
        return sServiceManager;
    }

    public static IBinder getService(String name) {
        return get().getServiceInternal(name);
    }

    private ServiceManager() {
        mCaches.put(ACTIVITY_MANAGER, PActivityManagerService.get());
        mCaches.put(JOB_MANAGER, PJobManagerService.get());
        mCaches.put(PACKAGE_MANAGER, PPackageManagerService.get());
        mCaches.put(STORAGE_MANAGER, PStorageManagerService.get());
        mCaches.put(USER_MANAGER, PUserManagerService.get());

        mCaches.put(ACCOUNT_MANAGER, PAccountManagerService.get());
        mCaches.put(LOCATION_MANAGER, PLocationManagerService.get());
        mCaches.put(NOTIFICATION_MANAGER, PNotificationManagerService.get());
    }

    public IBinder getServiceInternal(String name) {
        return mCaches.get(name);
    }

    public static void initBlackManager() {
        PrismSpaceCore.get().getService(ACTIVITY_MANAGER);
        PrismSpaceCore.get().getService(JOB_MANAGER);
        PrismSpaceCore.get().getService(PACKAGE_MANAGER);
        PrismSpaceCore.get().getService(STORAGE_MANAGER);
        PrismSpaceCore.get().getService(USER_MANAGER);

        PrismSpaceCore.get().getService(ACCOUNT_MANAGER);
        PrismSpaceCore.get().getService(LOCATION_MANAGER);
        PrismSpaceCore.get().getService(NOTIFICATION_MANAGER);
    }
}

