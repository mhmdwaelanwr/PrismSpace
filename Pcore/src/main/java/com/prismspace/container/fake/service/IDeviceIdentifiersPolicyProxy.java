package com.prismspace.container.fake.service;


import java.lang.reflect.Method;

import black.android.os.BRIDeviceIdentifiersPolicyServiceStub;
import black.android.os.BRServiceManager;
import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.fake.hook.PinderInvocationStub;
import com.prismspace.container.fake.hook.MethodHook;
import com.prismspace.container.fake.hook.ProxyMethod;
import com.prismspace.container.utils.Md5Utils;


public class IDeviceIdentifiersPolicyProxy extends PinderInvocationStub {

    public IDeviceIdentifiersPolicyProxy() {
        super(BRServiceManager.get().getService("device_identifiers"));
    }

    @Override
    protected Object getWho() {
        return BRIDeviceIdentifiersPolicyServiceStub.get().asInterface(BRServiceManager.get().getService("device_identifiers"));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("device_identifiers");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getSerialForPackage")
    public static class x extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {


            return Md5Utils.md5(PrismSpaceCore.getHostPkg());
        }
    }
}

