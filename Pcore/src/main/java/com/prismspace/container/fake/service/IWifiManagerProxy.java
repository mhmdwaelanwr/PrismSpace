package com.prismspace.container.fake.service;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.util.Log;

import java.lang.reflect.Method;

import black.android.net.wifi.BRIWifiManagerStub;
import black.android.net.wifi.BRWifiInfo;
import black.android.net.wifi.BRWifiSsid;
import black.android.os.BRServiceManager;
import com.prismspace.container.core.privacy.DeviceProfileManager;
import com.prismspace.container.fake.hook.PinderInvocationStub;
import com.prismspace.container.fake.hook.MethodHook;
import com.prismspace.container.fake.hook.ProxyMethod;


public class IWifiManagerProxy extends PinderInvocationStub {
    public static final String TAG = "IWifiManagerProxy";

    public IWifiManagerProxy() {
        super(BRServiceManager.get().getService(Context.WIFI_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIWifiManagerStub.get().asInterface(BRServiceManager.get().getService(Context.WIFI_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getConnectionInfo")
    public static class GetConnectionInfo extends MethodHook {
        private static final String DEFAULT_MAC = "ac:62:5a:82:65:c4";
        private static final String DEFAULT_SSID = "PrismSpace_Wifi";
        
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            WifiInfo wifiInfo = (WifiInfo) method.invoke(who, args);
            if (wifiInfo == null) {
                return null;
            }
            DeviceProfileManager.DeviceProfile profile = DeviceProfileManager.getProfileForCurrentApp();
            String customMac = profile != null ? profile.getMac() : null;
            String spoofedMac = customMac != null && !customMac.trim().isEmpty() ? customMac : DEFAULT_MAC;
            BRWifiInfo.get(wifiInfo)._set_mBSSID(spoofedMac);
            BRWifiInfo.get(wifiInfo)._set_mMacAddress(spoofedMac);
            BRWifiInfo.get(wifiInfo)._set_mWifiSsid(BRWifiSsid.get().createFromAsciiEncoded(DEFAULT_SSID));
            return wifiInfo;
        }

        public static String intIP2StringIP(int ip) {
            return (ip & 0xFF) + "." +
                    ((ip >> 8) & 0xFF) + "." +
                    ((ip >> 16) & 0xFF) + "." +
                    (ip >> 24 & 0xFF);
        }

        public static int ip2Int(String ipString) {
            
            String[] ipSlices = ipString.split("\\.");
            int rs = 0;
            for (int i = 0; i < ipSlices.length; i++) {
                
                int intSlice = Integer.parseInt(ipSlices[i]) << 8 * i;
                
                rs = rs | intSlice;
            }
            return rs;
        }
    }
}

