package com.prismspace.container.fake.service;

import android.content.Context;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import black.android.os.BRServiceManager;
import black.com.android.internal.telephony.BRITelephonyStub;
import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.app.PActivityThread;
import com.prismspace.container.core.privacy.DeviceProfileManager;
import com.prismspace.container.entity.location.PCell;
import com.prismspace.container.fake.frameworks.PLocationManager;
import com.prismspace.container.fake.hook.PinderInvocationStub;
import com.prismspace.container.fake.hook.MethodHook;
import com.prismspace.container.fake.hook.ProxyMethod;
import com.prismspace.container.utils.Md5Utils;


public class ITelephonyManagerProxy extends PinderInvocationStub {
    public static final String TAG = "ITelephonyManagerProxy";

    private static String resolveImeiOrDefault() {
        DeviceProfileManager.DeviceProfile profile = DeviceProfileManager.getProfileForCurrentApp();
        if (profile != null) {
            String customImei = profile.getImei();
            if (customImei != null && !customImei.trim().isEmpty()) {
                return customImei;
            }
        }
        return Md5Utils.md5(PrismSpaceCore.getHostPkg());
    }

    public ITelephonyManagerProxy() {
        super(BRServiceManager.get().getService(Context.TELEPHONY_SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder telephony = BRServiceManager.get().getService(Context.TELEPHONY_SERVICE);
        return BRITelephonyStub.get().asInterface(telephony);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getDeviceId")
    public static class GetDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return resolveImeiOrDefault();
        }
    }

    @ProxyMethod("getImeiForSlot")
    public static class getImeiForSlot extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return resolveImeiOrDefault();
        }
    }

    @ProxyMethod("getMeidForSlot")
    public static class GetMeidForSlot extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return resolveImeiOrDefault();
        }
    }

    @ProxyMethod("isUserDataEnabled")
    public static class IsUserDataEnabled extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return true;
        }
    }


    @ProxyMethod("getLine1NumberForDisplay")
    public static class getLine1NumberForDisplay extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    @ProxyMethod("getSubscriberId")
    public static class GetSubscriberId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return resolveImeiOrDefault();
        }
    }

    @ProxyMethod("getDeviceIdWithFeature")
    public static class GetDeviceIdWithFeature extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return resolveImeiOrDefault();
        }
    }

    @ProxyMethod("getCellLocation")
    public static class GetCellLocation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Log.d(TAG, "getCellLocation");
            if (PLocationManager.isFakeLocationEnable()) {
                PCell cell = PLocationManager.get().getCell(PActivityThread.getUserId(), PActivityThread.getAppPackageName());
                if (cell != null) {
                    
                    return null;
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getAllCellInfo")
    public static class GetAllCellInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (PLocationManager.isFakeLocationEnable()) {
                List<PCell> cell = PLocationManager.get().getAllCell(PActivityThread.getUserId(), PActivityThread.getAppPackageName());
                
                return cell;
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return null;
            }
        }
    }

    @ProxyMethod("getNetworkOperator")
    public static class GetNetworkOperator extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Log.d(TAG, "getNetworkOperator");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getNetworkTypeForSubscriber")
    public static class GetNetworkTypeForSubscriber extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return 0;
            }
        }
    }

    @ProxyMethod("getNeighboringCellInfo")
    public static class GetNeighboringCellInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Log.d(TAG, "getNeighboringCellInfo");
            if (PLocationManager.isFakeLocationEnable()) {
                List<PCell> cell = PLocationManager.get().getNeighboringCell(PActivityThread.getUserId(), PActivityThread.getAppPackageName());
                
                return null;
            }
            return method.invoke(who, args);
        }
    }
}

