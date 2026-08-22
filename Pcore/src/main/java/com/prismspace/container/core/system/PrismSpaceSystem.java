package com.prismspace.container.core.system;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.core.env.AppSystemEnv;
import com.prismspace.container.core.env.PEnvironment;
import com.prismspace.container.core.system.accounts.PAccountManagerService;
import com.prismspace.container.core.system.am.PActivityManagerService;
import com.prismspace.container.core.system.am.PJobManagerService;
import com.prismspace.container.core.system.location.PLocationManagerService;
import com.prismspace.container.core.system.notification.PNotificationManagerService;
import com.prismspace.container.core.system.os.PStorageManagerService;
import com.prismspace.container.core.system.pm.PPackageInstallerService;
import com.prismspace.container.core.system.pm.PPackageManagerService;

import com.prismspace.container.core.system.user.PUserHandle;
import com.prismspace.container.core.system.user.PUserManagerService;
import com.prismspace.container.entity.pm.InstallOption;
import com.prismspace.container.utils.FileUtils;

import com.prismspace.container.core.system.JarManager;


public class PrismSpaceSystem {
    private static PrismSpaceSystem sPrismSpaceSystem;
    private final List<ISystemService> mServices = new ArrayList<>();
    private final static AtomicBoolean isStartup = new AtomicBoolean(false);

    public static PrismSpaceSystem getSystem() {
        if (sPrismSpaceSystem == null) {
            synchronized (PrismSpaceSystem.class) {
                if (sPrismSpaceSystem == null) {
                    sPrismSpaceSystem = new PrismSpaceSystem();
                }
            }
        }
        return sPrismSpaceSystem;
    }

    public void startup() {
        if (isStartup.getAndSet(true))
            return;
        PEnvironment.load();

        mServices.add(PPackageManagerService.get());
        mServices.add(PUserManagerService.get());
        mServices.add(PActivityManagerService.get());
        mServices.add(PJobManagerService.get());
        mServices.add(PStorageManagerService.get());
        mServices.add(PPackageInstallerService.get());

        mServices.add(PProcessManagerService.get());
        mServices.add(PAccountManagerService.get());
        mServices.add(PLocationManagerService.get());
        mServices.add(PNotificationManagerService.get());

        for (ISystemService service : mServices) {
            service.systemReady();
        }

        List<String> preInstallPackages = AppSystemEnv.getPreInstallPackages();
        for (String preInstallPackage : preInstallPackages) {
            try {
                if (!PPackageManagerService.get().isInstalled(preInstallPackage, PUserHandle.USER_ALL)) {
                    PackageInfo packageInfo = PrismSpaceCore.getPackageManager().getPackageInfo(preInstallPackage, 0);
                    PPackageManagerService.get().installPackageAsUser(packageInfo.applicationInfo.sourceDir, InstallOption.installBySystem(), PUserHandle.USER_ALL);
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        
        JarManager.getInstance().initializeAsync();
        
        
     
    }
}

