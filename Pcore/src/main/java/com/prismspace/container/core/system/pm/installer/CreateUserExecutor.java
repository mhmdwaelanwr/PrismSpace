package com.prismspace.container.core.system.pm.installer;

import com.prismspace.container.core.env.PEnvironment;
import com.prismspace.container.core.system.pm.PPackageSettings;
import com.prismspace.container.entity.pm.InstallOption;
import com.prismspace.container.utils.FileUtils;


public class CreateUserExecutor implements Executor {

    @Override
    public int exec(PPackageSettings ps, InstallOption option, int userId) {
        String packageName = ps.pkg.packageName;
        FileUtils.deleteDir(PEnvironment.getDataLibDir(packageName, userId));

        
        FileUtils.mkdirs(PEnvironment.getDataDir(packageName, userId));
        FileUtils.mkdirs(PEnvironment.getDataCacheDir(packageName, userId));
        FileUtils.mkdirs(PEnvironment.getDataFilesDir(packageName, userId));
        FileUtils.mkdirs(PEnvironment.getDataDatabasesDir(packageName, userId));
        FileUtils.mkdirs(PEnvironment.getDeDataDir(packageName, userId));








        return 0;
    }
}

