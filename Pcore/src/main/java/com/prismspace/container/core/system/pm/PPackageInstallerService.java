package com.prismspace.container.core.system.pm;

import java.util.ArrayList;
import java.util.List;

import com.prismspace.container.core.system.ISystemService;
import com.prismspace.container.core.system.pm.installer.CopyExecutor;
import com.prismspace.container.core.system.pm.installer.CreatePackageExecutor;
import com.prismspace.container.core.system.pm.installer.CreateUserExecutor;
import com.prismspace.container.core.system.pm.installer.Executor;
import com.prismspace.container.core.system.pm.installer.RemoveAppExecutor;
import com.prismspace.container.core.system.pm.installer.RemoveUserExecutor;
import com.prismspace.container.entity.pm.InstallOption;
import com.prismspace.container.utils.Slog;


public class PPackageInstallerService extends IBPackageInstallerService.Stub implements ISystemService {
    private static final PPackageInstallerService sService = new PPackageInstallerService();

    public static PPackageInstallerService get() {
        return sService;
    }

    public static final String TAG = "PPackageInstallerService";

    @Override
    public int installPackageAsUser(PPackageSettings ps, int userId) {
        List<Executor> executors = new ArrayList<>();
        
        executors.add(new CreateUserExecutor());
        
        executors.add(new CreatePackageExecutor());
        
        executors.add(new CopyExecutor());
        InstallOption option = ps.installOption;
        for (Executor executor : executors) {
            int exec = executor.exec(ps, option, userId);
            Slog.d(TAG, "installPackageAsUser: " + executor.getClass().getSimpleName() + " exec: " + exec);
            if (exec != 0) {
                return exec;
            }
        }
        return 0;
    }

    @Override
    public int uninstallPackageAsUser(PPackageSettings ps, boolean removeApp, int userId) {
        List<Executor> executors = new ArrayList<>();
        if (removeApp) {
            
            executors.add(new RemoveAppExecutor());
        }
        
        executors.add(new RemoveUserExecutor());
        InstallOption option = ps.installOption;
        for (Executor executor : executors) {
            int exec = executor.exec(ps, option, userId);
            Slog.d(TAG, "uninstallPackageAsUser: " + executor.getClass().getSimpleName() + " exec: " + exec);
            if (exec != 0) {
                return exec;
            }
        }
        return 0;
    }

    @Override
    public int clearPackage(PPackageSettings ps, int userId) {
        List<Executor> executors = new ArrayList<>();
        
        executors.add(new RemoveUserExecutor());
        
        executors.add(new CreateUserExecutor());
        InstallOption option = ps.installOption;
        for (Executor executor : executors) {
            int exec = executor.exec(ps, option, userId);
            Slog.d(TAG, "uninstallPackageAsUser: " + executor.getClass().getSimpleName() + " exec: " + exec);
            if (exec != 0) {
                return exec;
            }
        }
        return 0;
    }

    @Override
    public int updatePackage(PPackageSettings ps) {
        List<Executor> executors = new ArrayList<>();
        executors.add(new CreatePackageExecutor());
        executors.add(new CopyExecutor());
        InstallOption option = ps.installOption;
        for (Executor executor : executors) {
            int exec = executor.exec(ps, option, -1);
            if (exec != 0) {
                return exec;
            }
        }
        return 0;
    }

    @Override
    public void systemReady() {

    }
}

