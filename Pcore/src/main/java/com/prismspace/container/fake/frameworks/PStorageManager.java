package com.prismspace.container.fake.frameworks;

import android.net.Uri;
import android.os.RemoteException;
import android.os.storage.StorageVolume;

import com.prismspace.container.core.system.ServiceManager;
import com.prismspace.container.core.system.os.IBStorageManagerService;


public class PStorageManager extends PlackManager<IBStorageManagerService> {
    private static final PStorageManager sStorageManager = new PStorageManager();

    public static PStorageManager get() {
        return sStorageManager;
    }

    @Override
    protected String getServiceName() {
        return ServiceManager.STORAGE_MANAGER;
    }

    public StorageVolume[] getVolumeList(int uid, String packageName, int flags, int userId) {
        try {
            return getService().getVolumeList(uid, packageName, flags, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return new StorageVolume[]{};
    }

    public Uri getUriForFile(String file) {
        try {
            return getService().getUriForFile(file);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }
}

