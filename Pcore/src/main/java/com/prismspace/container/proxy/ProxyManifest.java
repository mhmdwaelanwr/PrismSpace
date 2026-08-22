package com.prismspace.container.proxy;

import java.util.Locale;

import com.prismspace.container.PrismSpaceCore;


public class ProxyManifest {
    public static final int FREE_COUNT = 50;

    public static boolean isProxy(String msg) {
        return getBindProvider().equals(msg) || msg.contains("proxy_content_provider_");
    }

    public static String getBindProvider() {
        return PrismSpaceCore.getHostPkg() + ".prismspace.SystemCallProvider";
    }

    public static String getProxyAuthorities(int index) {
        return String.format(Locale.CHINA, "%s.proxy_content_provider_%d", PrismSpaceCore.getHostPkg(), index);
    }

    public static String getProxyPendingActivity(int index) {
        return String.format(Locale.CHINA, "com.prismspace.container.proxy.ProxyPendingActivity$P%d", index);
    }

    public static String getProxyActivity(int index) {
        return String.format(Locale.CHINA, "com.prismspace.container.proxy.ProxyActivity$P%d", index);
    }

    public static String TransparentProxyActivity(int index) {
        return String.format(Locale.CHINA, "com.prismspace.container.proxy.TransparentProxyActivity$P%d", index);
    }

    public static String getProxyService(int index) {
        return String.format(Locale.CHINA, "com.prismspace.container.proxy.ProxyService$P%d", index);
    }

    public static String getProxyJobService(int index) {
        return String.format(Locale.CHINA, "com.prismspace.container.proxy.ProxyJobService$P%d", index);
    }

    public static String getProxyFileProvider() {
        return PrismSpaceCore.getHostPkg() + ".prismspace.FileProvider";
    }

    public static String getProxyReceiver() {
        return PrismSpaceCore.getHostPkg() + ".stub_receiver";
    }

    public static String getProcessName(int bPid) {
        return PrismSpaceCore.getHostPkg() + ":p" + bPid;
    }
}

