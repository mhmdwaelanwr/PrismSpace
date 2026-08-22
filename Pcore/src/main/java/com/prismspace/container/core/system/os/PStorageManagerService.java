package com.prismspace.container.core.system.os;

import android.net.Uri;
import android.os.Process;
import android.os.RemoteException;
import android.os.storage.StorageVolume;

import java.io.File;

import black.android.os.storage.BRStorageManager;
import black.android.os.storage.BRStorageVolume;
import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.core.env.PEnvironment;
import com.prismspace.container.core.system.ISystemService;
import com.prismspace.container.core.system.user.PUserHandle;
import com.prismspace.container.fake.provider.FileProvider;
import com.prismspace.container.proxy.ProxyManifest;
import com.prismspace.container.utils.compat.PuildCompat;


public class PStorageManagerService extends IBStorageManagerService.Stub implements ISystemService {
    private static final PStorageManagerService sService = new PStorageManagerService();

    public static PStorageManagerService get() {
        return sService;
    }

    public PStorageManagerService() {
    }

    @Override
    public StorageVolume[] getVolumeList(int uid, String packageName, int flags, int userId) throws RemoteException {
        if (BRStorageManager.get().getVolumeList(0, 0) == null) {
            return null;
        }
        try {
            StorageVolume[] storageVolumes = BRStorageManager.get().getVolumeList(PUserHandle.getUserId(Process.myUid()), 0);
            if (storageVolumes == null)
                return null;
            for (StorageVolume storageVolume : storageVolumes) {
                BRStorageVolume.get(storageVolume)._set_mPath(PEnvironment.getExternalUserDir(userId));
                if (PuildCompat.isPie()) {
                    BRStorageVolume.get(storageVolume)._set_mInternalPath(PEnvironment.getExternalUserDir(userId));
                }
            }
            return storageVolumes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Uri getUriForFile(String file) throws RemoteException {
        return FileProvider.getUriForFile(PrismSpaceCore.getContext(), ProxyManifest.getProxyFileProvider(), new File(file));
    }

    @Override
    public void systemReady() {

    }
}

