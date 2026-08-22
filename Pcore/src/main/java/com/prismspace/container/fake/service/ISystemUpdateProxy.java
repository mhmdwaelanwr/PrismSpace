package com.prismspace.container.fake.service;


import black.android.os.BRServiceManager;
import black.android.view.BRIAutoFillManagerStub;
import com.prismspace.container.fake.hook.PinderInvocationStub;


public class ISystemUpdateProxy extends PinderInvocationStub {
    public ISystemUpdateProxy() {
        super(BRServiceManager.get().getService("system_update"));
    }

    @Override
    protected Object getWho() {
        return BRIAutoFillManagerStub.get().asInterface(BRServiceManager.get().getService("system_update"));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("system_update");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}

