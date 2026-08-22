package com.prismspace.container.fake.service;

import android.content.Context;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRIVibratorManagerServiceStub;
import black.android.os.BRServiceManager;
import black.com.android.internal.os.BRIVibratorServiceStub;
import com.prismspace.container.fake.hook.PinderInvocationStub;
import com.prismspace.container.utils.MethodParameterUtils;
import com.prismspace.container.utils.compat.PuildCompat;


public class IVibratorServiceProxy extends PinderInvocationStub {
    private static String NAME;
    static {
        if (PuildCompat.isS()) {
            NAME = "vibrator_manager";
        } else {
            NAME = Context.VIBRATOR_SERVICE;
        }
    }

    public IVibratorServiceProxy() {
        super(BRServiceManager.get().getService(NAME));
    }

    @Override
    protected Object getWho() {
        IBinder service = BRServiceManager.get().getService(NAME);
        if (PuildCompat.isS()) {
            return BRIVibratorManagerServiceStub.get().asInterface(service);
        }
        return BRIVibratorServiceStub.get().asInterface(service);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(NAME);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodParameterUtils.replaceFirstUid(args);
        MethodParameterUtils.replaceFirstAppPkg(args);
        return super.invoke(proxy, method, args);
    }
}

