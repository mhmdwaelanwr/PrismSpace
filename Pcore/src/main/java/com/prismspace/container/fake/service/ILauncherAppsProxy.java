package com.prismspace.container.fake.service;

import android.content.Context;

import java.lang.reflect.Method;

import black.android.content.pm.BRILauncherAppsStub;
import black.android.os.BRServiceManager;
import com.prismspace.container.fake.hook.PinderInvocationStub;
import com.prismspace.container.utils.MethodParameterUtils;


public class ILauncherAppsProxy extends PinderInvocationStub {

    public ILauncherAppsProxy() {
        super(BRServiceManager.get().getService(Context.LAUNCHER_APPS_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRILauncherAppsStub.get().asInterface(BRServiceManager.get().getService(Context.LAUNCHER_APPS_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.LAUNCHER_APPS_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodParameterUtils.replaceFirstAppPkg(args);
        
        return super.invoke(proxy, method, args);
    }

}

