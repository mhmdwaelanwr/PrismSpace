package com.prismspace.container.fake.service;

import android.content.Context;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import com.prismspace.container.fake.hook.PinderInvocationStub;
import com.prismspace.container.fake.hook.MethodHook;
import com.prismspace.container.fake.hook.ProxyMethod;
import com.prismspace.container.fake.hook.ScanClass;


@ScanClass(IInputMethodManagerProxy.class)
public class IInputMethodManagerProxy extends PinderInvocationStub {
    public static final String TAG = "IInputMethodManagerProxy";

    public IInputMethodManagerProxy() {
        super(BRServiceManager.get().getService(Context.INPUT_METHOD_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRServiceManager.get().getService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("startInputOrWindowGainedFocus")
    public static class StartInputOrWindowGainedFocus extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return method.invoke(who, args);
        }
    }
}

