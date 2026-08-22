package com.prismspace.container.fake.service.base;

import java.lang.reflect.Method;

import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.app.PActivityThread;
import com.prismspace.container.fake.hook.MethodHook;


public class UidMethodProxy extends MethodHook {
    private final int index;
    private final String name;

    public UidMethodProxy(String name, int index) {
        this.index = index;
        this.name = name;
    }

    @Override
    protected String getMethodName() {
        return name;
    }

    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
        int uid = (int) args[index];
        if (uid == PActivityThread.getBUid()) {
            args[index] = PrismSpaceCore.getHostUid();
        }
        return method.invoke(who, args);
    }
}

