package com.prismspace.container.fake.service;

import java.lang.reflect.Method;
import java.util.List;

import black.com.android.internal.net.BRVpnConfig;
import black.com.android.internal.net.VpnConfigContext;
import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.app.PActivityThread;
import com.prismspace.container.fake.hook.MethodHook;
import com.prismspace.container.fake.hook.ProxyMethod;
import com.prismspace.container.proxy.ProxyVpnService;
import com.prismspace.container.utils.MethodParameterUtils;


public class VpnCommonProxy {
    @ProxyMethod("setVpnPackageAuthorization")
    public static class setVpnPackageAuthorization extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("prepareVpn")
    public static class PrepareVpn extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("establishVpn")
    public static class establishVpn extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            VpnConfigContext vpnConfigContext = BRVpnConfig.get(args[0]);
            vpnConfigContext._set_user(ProxyVpnService.class.getName());

            handlePackage(vpnConfigContext.allowedApplications());
            handlePackage(vpnConfigContext.disallowedApplications());
            return method.invoke(who, args);
        }

        private void handlePackage(List<String> applications) {
            if (applications == null)
                return;
            if (applications.contains(PActivityThread.getAppPackageName())) {
                applications.add(PrismSpaceCore.getHostPkg());
            }
        }
    }

}

