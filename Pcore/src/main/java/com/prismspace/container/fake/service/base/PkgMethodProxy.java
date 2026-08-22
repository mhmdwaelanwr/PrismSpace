package com.prismspace.container.fake.service.base;

import java.lang.reflect.Method;

import com.prismspace.container.fake.hook.MethodHook;
import com.prismspace.container.utils.MethodParameterUtils;

public class PkgMethodProxy extends MethodHook {

	String mName;

	public PkgMethodProxy(String name) {
		mName = name;
	}

	@Override
	protected String getMethodName() {
		return mName;
	}

	@Override
	protected Object hook(Object who, Method method, Object[] args) throws Throwable {
		MethodParameterUtils.replaceFirstAppPkg(args);
		return method.invoke(who, args);
	}
}
