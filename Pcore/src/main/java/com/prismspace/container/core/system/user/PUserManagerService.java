package com.prismspace.container.core.system.user;

import android.os.Parcel;
import android.os.RemoteException;

import androidx.core.util.AtomicFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.prismspace.container.core.env.PEnvironment;
import com.prismspace.container.core.system.ISystemService;
import com.prismspace.container.core.system.pm.PPackageManagerService;
import com.prismspace.container.utils.CloseUtils;
import com.prismspace.container.utils.FileUtils;


public class PUserManagerService extends IBUserManagerService.Stub implements ISystemService {
    private static PUserManagerService sService = new PUserManagerService();
    public final HashMap<Integer, PUserInfo> mUsers = new HashMap<>();
    public final Object mUserLock = new Object();

    public static PUserManagerService get() {
        return sService;
    }

    @Override
    public void systemReady() {
        scanUserL();
    }

    @Override
    public PUserInfo getUserInfo(int userId) {
        synchronized (mUserLock) {
            return mUsers.get(userId);
        }
    }

    @Override
    public boolean exists(int userId) {
        synchronized (mUsers) {
            return mUsers.get(userId) != null;
        }
    }

    @Override
    public PUserInfo createUser(int userId) throws RemoteException {
        synchronized (mUserLock) {
            if (exists(userId)) {
                return getUserInfo(userId);
            }
            return createUserLocked(userId);
        }
    }

    @Override
    public List<PUserInfo> getUsers() {
        synchronized (mUsers) {
            ArrayList<PUserInfo> bUsers = new ArrayList<>();
            for (PUserInfo value : mUsers.values()) {
                if (value.id >= 0) {
                    bUsers.add(value);
                }
            }
            return bUsers;
        }
    }

    public List<PUserInfo> getAllUsers() {
        synchronized (mUsers) {
            return new ArrayList<>(mUsers.values());
        }
    }

    @Override
    public void deleteUser(int userId) throws RemoteException {
        synchronized (mUserLock) {
            synchronized (mUsers) {
                PPackageManagerService.get().deleteUser(userId);

                mUsers.remove(userId);
                saveUserInfoLocked();
                FileUtils.deleteDir(PEnvironment.getUserDir(userId));
                FileUtils.deleteDir(PEnvironment.getExternalUserDir(userId));
            }
        }
    }

    private PUserInfo createUserLocked(int userId) {
        PUserInfo bUserInfo = new PUserInfo();
        bUserInfo.id = userId;
        bUserInfo.status = PUserStatus.ENABLE;
        mUsers.put(userId, bUserInfo);
        synchronized (mUsers) {
            saveUserInfoLocked();
        }
        return bUserInfo;
    }

    private void saveUserInfoLocked() {
        Parcel parcel = Parcel.obtain();
        AtomicFile atomicFile = new AtomicFile(PEnvironment.getUserInfoConf());
        FileOutputStream fileOutputStream = null;
        try {
            ArrayList<PUserInfo> bUsers = new ArrayList<>(mUsers.values());
            parcel.writeTypedList(bUsers);
            try {
                fileOutputStream = atomicFile.startWrite();
                FileUtils.writeParcelToOutput(parcel, fileOutputStream);
                atomicFile.finishWrite(fileOutputStream);
            } catch (IOException e) {
                e.printStackTrace();
                atomicFile.failWrite(fileOutputStream);
            } finally {
                CloseUtils.close(fileOutputStream);
            }
        } finally {
            parcel.recycle();
        }
    }

    private void scanUserL() {
        synchronized (mUserLock) {
            Parcel parcel = Parcel.obtain();
            InputStream is = null;
            try {
                File userInfoConf = PEnvironment.getUserInfoConf();
                if (!userInfoConf.exists()) {
                    return;
                }
                is = new FileInputStream(PEnvironment.getUserInfoConf());
                byte[] bytes = FileUtils.toByteArray(is);
                parcel.unmarshall(bytes, 0, bytes.length);
                parcel.setDataPosition(0);

                ArrayList<PUserInfo> loadUsers = parcel.createTypedArrayList(PUserInfo.CREATOR);
                if (loadUsers == null)
                    return;
                synchronized (mUsers) {
                    mUsers.clear();
                    for (PUserInfo loadUser : loadUsers) {
                        mUsers.put(loadUser.id, loadUser);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                parcel.recycle();
                CloseUtils.close(is);
            }
        }
    }
}
