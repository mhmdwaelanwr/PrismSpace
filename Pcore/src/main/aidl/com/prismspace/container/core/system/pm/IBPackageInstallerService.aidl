// IBPackageInstallerService.aidl
package com.prismspace.container.core.system.pm;

import com.prismspace.container.core.system.pm.PPackageSettings;
import com.prismspace.container.entity.pm.InstallOption;

// Declare any non-default types here with import statements

interface IBPackageInstallerService {
    int installPackageAsUser(in PPackageSettings ps, int userId);
    int uninstallPackageAsUser(in PPackageSettings ps, boolean removeApp, int userId);
    int clearPackage(in PPackageSettings ps, int userId);
    int updatePackage(in PPackageSettings ps);
}

