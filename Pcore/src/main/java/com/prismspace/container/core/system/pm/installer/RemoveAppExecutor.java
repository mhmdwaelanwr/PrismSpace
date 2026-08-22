package com.prismspace.container.core.system.pm.installer;

import com.prismspace.container.core.env.PEnvironment;
import com.prismspace.container.core.system.pm.PPackageSettings;
import com.prismspace.container.entity.pm.InstallOption;
import com.prismspace.container.utils.FileUtils;


public class RemoveAppExecutor implements Executor {
    @Override
    public int exec(PPackageSettings ps, InstallOption option, int userId) {
        FileUtils.deleteDir(PEnvironment.getAppDir(ps.pkg.packageName));
        return 0;
    }
}

