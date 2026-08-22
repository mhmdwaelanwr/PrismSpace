package com.prismspace.container.fake.service;

import android.os.IInterface;
import android.os.storage.StorageVolume;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import black.android.os.mount.BRIMountServiceStub;
import black.android.os.storage.BRIStorageManagerStub;
import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.app.PActivityThread;
import com.prismspace.container.fake.hook.PinderInvocationStub;
import com.prismspace.container.fake.hook.MethodHook;
import com.prismspace.container.fake.hook.ProxyMethod;
import com.prismspace.container.utils.compat.PuildCompat;


public class IStorageManagerProxy extends PinderInvocationStub {

    public IStorageManagerProxy() {
        super(BRServiceManager.get().getService("mount"));
    }

    @Override
    protected Object getWho() {
        IInterface mount;
        if (PuildCompat.isOreo()) {
            mount = BRIStorageManagerStub.get().asInterface(BRServiceManager.get().getService("mount"));
        } else {
            mount = BRIMountServiceStub.get().asInterface(BRServiceManager.get().getService("mount"));
        }
        return mount;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("mount");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getVolumeList")
    public static class GetVolumeList extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args == null) {
                StorageVolume[] volumeList = PrismSpaceCore.getBStorageManager().getVolumeList(PActivityThread.getBUid(), null, 0, PActivityThread.getUserId());
                if (volumeList == null) {
                    return method.invoke(who, args);
                }
                return volumeList;
            }
            try {
                int uid = (int) args[0];
                String packageName = (String) args[1];
                int flags = (int) args[2];
                StorageVolume[] volumeList = PrismSpaceCore.getBStorageManager().getVolumeList(uid, packageName, flags, PActivityThread.getUserId());
                if (volumeList == null) {
                    return method.invoke(who, args);
                }
                return volumeList;
            } catch (Throwable t) {
                return method.invoke(who, args);
            }
        }
    }

    @ProxyMethod("mkdirs")
    public static class mkdirs extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }
}

