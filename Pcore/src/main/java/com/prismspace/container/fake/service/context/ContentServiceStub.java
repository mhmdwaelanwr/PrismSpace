package com.prismspace.container.fake.service.context;

import java.lang.reflect.Method;

import black.android.content.BRIContentServiceStub;
import black.android.os.BRServiceManager;
import com.prismspace.container.fake.hook.PinderInvocationStub;
import com.prismspace.container.fake.hook.MethodHook;
import com.prismspace.container.fake.hook.ProxyMethod;


public class ContentServiceStub extends PinderInvocationStub {

    public ContentServiceStub() {
        super(BRServiceManager.get().getService("content"));
    }

    @Override
    protected Object getWho() {
        return BRIContentServiceStub.get().asInterface(BRServiceManager.get().getService("content"));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("content");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("registerContentObserver")
    public static class RegisterContentObserver extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }

    @ProxyMethod("notifyChange")
    public static class NotifyChange extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }
}

