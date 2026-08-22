package com.prismspace.container.fake.service;

import android.content.Context;
import android.location.LocationManager;
import android.os.IInterface;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import black.android.location.BRILocationListener;
import black.android.location.BRILocationManagerStub;
import black.android.location.provider.BRProviderProperties;
import black.android.location.provider.ProviderProperties;
import black.android.os.BRServiceManager;
import com.prismspace.container.app.PActivityThread;
import com.prismspace.container.entity.location.PLocation;
import com.prismspace.container.fake.frameworks.PLocationManager;
import com.prismspace.container.fake.hook.PinderInvocationStub;
import com.prismspace.container.fake.hook.MethodHook;
import com.prismspace.container.fake.hook.ProxyMethod;
import com.prismspace.container.utils.MethodParameterUtils;


public class ILocationManagerProxy extends PinderInvocationStub {
    public static final String TAG = "ILocationManagerProxy";

    public ILocationManagerProxy() {
        super(BRServiceManager.get().getService(Context.LOCATION_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRILocationManagerStub.get().asInterface(BRServiceManager.get().getService(Context.LOCATION_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        MethodParameterUtils.replaceFirstAppPkg(args);
        
        
        String packageName = PActivityThread.getAppPackageName();
        if (packageName != null && packageName.equals("com.google.android.gms")) {
            
            if (method.getName().equals("getLastLocation") || 
                method.getName().equals("getLastKnownLocation") ||
                method.getName().equals("requestLocationUpdates")) {
                Log.w(TAG, "Blocking location request from Google Play Services to prevent crash");
                return null;
            }
        }
        
        return super.invoke(proxy, method, args);
    }

    @ProxyMethod("registerGnssStatusCallback")
    public static class RegisterGnssStatusCallback extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            return true;
        }
    }

    @ProxyMethod("getLastLocation")
    public static class GetLastLocation extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (PLocationManager.isFakeLocationEnable()) {
                return PLocationManager.get().getLocation(PActivityThread.getUserId(), PActivityThread.getAppPackageName()).convert2SystemLocation();
            }
            
            
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                if (e.getCause() instanceof SecurityException) {
                    Log.w(TAG, "Location permission denied, returning null for getLastLocation");
                    return null;
                }
                throw e;
            }
        }
    }

    @ProxyMethod("getLastKnownLocation")
    public static class GetLastKnownLocation extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (PLocationManager.isFakeLocationEnable()) {
                return PLocationManager.get().getLocation(PActivityThread.getUserId(), PActivityThread.getAppPackageName()).convert2SystemLocation();
            }
            
            
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                if (e.getCause() instanceof SecurityException) {
                    Log.w(TAG, "Location permission denied, returning null for getLastKnownLocation");
                    return null;
                }
                throw e;
            }
        }
    }

    @ProxyMethod("requestLocationUpdates")
    public static class RequestLocationUpdates extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (PLocationManager.isFakeLocationEnable()) {
                if (args[1] instanceof IInterface) {
                    IInterface listener = (IInterface) args[1];
                    PLocationManager.get().requestLocationUpdates(listener.asBinder());
                    return 0;
                }
            }
            
            
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                if (e.getCause() instanceof SecurityException) {
                    Log.w(TAG, "Location permission denied for requestLocationUpdates, returning 0");
                    return 0;
                }
                throw e;
            }
        }
    }

    @ProxyMethod("removeUpdates")
    public static class RemoveUpdates extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args[0] instanceof IInterface) {
                IInterface listener = (IInterface) args[0];
                PLocationManager.get().removeUpdates(listener.asBinder());
                return 0;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getProviderProperties")
    public static class GetProviderProperties extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object providerProperties = method.invoke(who, args);
            if (PLocationManager.isFakeLocationEnable()) {
                BRProviderProperties.get(providerProperties)._set_mHasNetworkRequirement(false);
                if (PLocationManager.get().getCell(PActivityThread.getUserId(), PActivityThread.getAppPackageName()) == null) {
                    BRProviderProperties.get(providerProperties)._set_mHasCellRequirement(false);
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("removeGpsStatusListener")
    public static class RemoveGpsStatusListener extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            return 0;
        }
    }

    @ProxyMethod("getBestProvider")
    public static class GetBestProvider extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (PLocationManager.isFakeLocationEnable()) {
                return LocationManager.GPS_PROVIDER;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getAllProviders")
    public static class GetAllProviders extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return Arrays.asList(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER);
        }
    }

    @ProxyMethod("isProviderEnabledForUser")
    public static class isProviderEnabledForUser extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String provider = (String) args[0];
            return Objects.equals(provider, LocationManager.GPS_PROVIDER);
        }
    }

    @ProxyMethod("setExtraLocationControllerPackageEnabled")
    public static class setExtraLocationControllerPackageEnabled extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }
}

