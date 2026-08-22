package com.prismspace.container.fake.delegate;

import android.net.Uri;
import android.os.Build;
import android.os.IInterface;
import android.util.ArrayMap;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import black.android.app.BRActivityThread;
import black.android.app.BRActivityThreadProviderClientRecordP;
import black.android.app.BRIActivityManagerContentProviderHolder;
import black.android.content.BRContentProviderHolderOreo;
import black.android.providers.BRSettingsContentProviderHolder;
import black.android.providers.BRSettingsGlobal;
import black.android.providers.BRSettingsNameValueCache;
import black.android.providers.BRSettingsNameValueCacheOreo;
import black.android.providers.BRSettingsSecure;
import black.android.providers.BRSettingsSystem;
import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.fake.service.context.providers.ContentProviderStub;
import com.prismspace.container.fake.service.context.providers.SystemProviderStub;
import com.prismspace.container.utils.compat.PuildCompat;


public class ContentProviderDelegate {
    public static final String TAG = "ContentProviderDelegate";
    private static Set<String> sInjected = new HashSet<>();

    public static void update(Object holder, String auth) {
        IInterface iInterface;
        if (PuildCompat.isOreo()) {
            iInterface = BRContentProviderHolderOreo.get(holder).provider();
        } else {
            iInterface = BRIActivityManagerContentProviderHolder.get(holder).provider();
        }

        if (iInterface instanceof Proxy)
            return;
        IInterface bContentProvider;
        switch (auth) {
            case "media":
            case "telephony":
            case "settings":
                bContentProvider = new SystemProviderStub().wrapper(iInterface, PrismSpaceCore.getHostPkg());
                break;
            default:
                bContentProvider = new ContentProviderStub().wrapper(iInterface, PrismSpaceCore.getHostPkg());
                break;
        }
        if (PuildCompat.isOreo()) {
            BRContentProviderHolderOreo.get(holder)._set_provider(bContentProvider);
        } else {
            BRIActivityManagerContentProviderHolder.get(holder)._set_provider(bContentProvider);
        }
    }

    public static void init() {
        clearSettingProvider();

        PrismSpaceCore.getContext().getContentResolver().call(Uri.parse("content://settings"), "", null, null);
        Object activityThread = PrismSpaceCore.mainThread();
        ArrayMap<Object, Object> map = (ArrayMap<Object, Object>) BRActivityThread.get(activityThread).mProviderMap();

        for (Object value : map.values()) {
            String[] mNames = BRActivityThreadProviderClientRecordP.get(value).mNames();
            if (mNames == null || mNames.length <= 0) {
                continue;
            }
            String providerName = mNames[0];
            if (!sInjected.contains(providerName)) {
                sInjected.add(providerName);
                final IInterface iInterface = BRActivityThreadProviderClientRecordP.get(value).mProvider();
                BRActivityThreadProviderClientRecordP.get(value)._set_mProvider(new ContentProviderStub().wrapper(iInterface, PrismSpaceCore.getHostPkg()));
                BRActivityThreadProviderClientRecordP.get(value)._set_mNames(new String[]{providerName});
            }
        }
    }

    public static void clearSettingProvider() {
        Object cache;
        cache = BRSettingsSystem.get().sNameValueCache();
        if (cache != null) {
            clearContentProvider(cache);
        }
        cache = BRSettingsSecure.get().sNameValueCache();
        if (cache != null) {
            clearContentProvider(cache);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && BRSettingsGlobal.getRealClass() != null) {
            cache = BRSettingsGlobal.get().sNameValueCache();
            if (cache != null) {
                clearContentProvider(cache);
            }
        }
    }

    private static void clearContentProvider(Object cache) {
        if (PuildCompat.isOreo()) {
            Object holder = BRSettingsNameValueCacheOreo.get(cache).mProviderHolder();
            if (holder != null) {
                BRSettingsContentProviderHolder.get(holder)._set_mContentProvider(null);
            }
        } else {
            BRSettingsNameValueCache.get(cache)._set_mContentProvider(null);
        }
    }
}

