// IBUserManagerService.aidl
package com.prismspace.container.core.system.user;

// Declare any non-default types here with import statements
import com.prismspace.container.core.system.user.PUserInfo;
import java.util.List;


interface IBUserManagerService {
    PUserInfo getUserInfo(int userId);
    boolean exists(int userId);
    PUserInfo createUser(int userId);
    List<PUserInfo> getUsers();
    void deleteUser(int userId);
}

