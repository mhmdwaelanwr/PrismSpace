package com.prismspace.container.core.system.pm.installer;

import com.prismspace.container.core.env.PEnvironment;
import com.prismspace.container.core.system.pm.PPackageSettings;
import com.prismspace.container.entity.pm.InstallOption;
import com.prismspace.container.utils.FileUtils;


public class RemoveUserExecutor implements Executor {

    @Override
    public int exec(PPackageSettings ps, InstallOption option, int userId) {
        String packageName = ps.pkg.packageName;
        
        FileUtils.deleteDir(PEnvironment.getDataDir(packageName, userId));
        FileUtils.deleteDir(PEnvironment.getDeDataDir(packageName, userId));
        FileUtils.deleteDir(PEnvironment.getExternalDataDir(packageName, userId));
        return 0;
    }
}

