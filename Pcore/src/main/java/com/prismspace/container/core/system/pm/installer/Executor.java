package com.prismspace.container.core.system.pm.installer;

import com.prismspace.container.core.system.pm.PPackageSettings;
import com.prismspace.container.entity.pm.InstallOption;


public interface Executor {
    public static final String TAG = "InstallExecutor";

    int exec(PPackageSettings ps, InstallOption option, int userId);
}

