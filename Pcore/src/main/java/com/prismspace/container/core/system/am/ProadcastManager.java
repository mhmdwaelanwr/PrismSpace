package com.prismspace.container.core.system.am;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.core.system.pm.PPackage;
import com.prismspace.container.core.system.pm.PPackageManagerService;
import com.prismspace.container.core.system.pm.PPackageSettings;
import com.prismspace.container.core.system.pm.PackageMonitor;
import com.prismspace.container.entity.am.PendingResultData;
import com.prismspace.container.proxy.ProxyBroadcastReceiver;
import com.prismspace.container.utils.Slog;


public class ProadcastManager implements PackageMonitor {
    public static final String TAG = "ProadcastManager";

    public static final int TIMEOUT = 9000;

    public static final int MSG_TIME_OUT = 1;

    private static ProadcastManager sBroadcastManager;

    private final PActivityManagerService mAms;
    private final PPackageManagerService mPms;
    private final Map<String, List<BroadcastReceiver>> mReceivers = new HashMap<>();
    private final Map<String, PendingResultData> mReceiversData = new HashMap<>();

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_TIME_OUT:
                    try {
                        PendingResultData data = (PendingResultData) msg.obj;
                        data.build().finish();
                        Slog.d(TAG, "Timeout Receiver: " + data);
                    } catch (Throwable ignore) {
                    }
                    break;
            }
        }
    };

    public static ProadcastManager startSystem(PActivityManagerService ams, PPackageManagerService pms) {
        if (sBroadcastManager == null) {
            synchronized (ProadcastManager.class) {
                if (sBroadcastManager == null) {
                    sBroadcastManager = new ProadcastManager(ams, pms);
                }
            }
        }
        return sBroadcastManager;
    }

    public ProadcastManager(PActivityManagerService ams, PPackageManagerService pms) {
        mAms = ams;
        mPms = pms;
    }

    public void startup() {
        mPms.addPackageMonitor(this);
        List<PPackageSettings> bPackageSettings = mPms.getBPackageSettings();
        for (PPackageSettings bPackageSetting : bPackageSettings) {
            PPackage bPackage = bPackageSetting.pkg;
            registerPackage(bPackage);
        }
    }

    private void registerPackage(PPackage bPackage) {
        synchronized (mReceivers) {
            Slog.d(TAG, "register: " + bPackage.packageName + ", size: " + bPackage.receivers.size());
            for (PPackage.Activity receiver : bPackage.receivers) {
                List<PPackage.ActivityIntentInfo> intents = receiver.intents;
                for (PPackage.ActivityIntentInfo intent : intents) {
                    ProxyBroadcastReceiver proxyBroadcastReceiver = new ProxyBroadcastReceiver();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        PrismSpaceCore.getContext().registerReceiver(proxyBroadcastReceiver, intent.intentFilter, Context.RECEIVER_EXPORTED);
                    }else{
                        PrismSpaceCore.getContext().registerReceiver(proxyBroadcastReceiver, intent.intentFilter);
                    }
                    addReceiver(bPackage.packageName, proxyBroadcastReceiver);
                }
            }
        }
    }

    private void addReceiver(String packageName, BroadcastReceiver receiver) {
        List<BroadcastReceiver> broadcastReceivers = mReceivers.get(packageName);
        if (broadcastReceivers == null) {
            broadcastReceivers = new ArrayList<>();
            mReceivers.put(packageName, broadcastReceivers);
        }
        broadcastReceivers.add(receiver);
    }

    public void sendBroadcast(PendingResultData pendingResultData) {
        synchronized (mReceiversData) {
            
            mReceiversData.put(pendingResultData.mBToken, pendingResultData);
            Message obtain = Message.obtain(mHandler, MSG_TIME_OUT, pendingResultData);
            mHandler.sendMessageDelayed(obtain, TIMEOUT);
        }
    }

    public void finishBroadcast(PendingResultData data) {
        synchronized (mReceiversData) {
            
            mHandler.removeMessages(MSG_TIME_OUT, mReceiversData.get(data.mBToken));
        }
    }

    @Override
    public void onPackageUninstalled(String packageName, boolean removeApp, int userId) {
        if (removeApp) {
            synchronized (mReceivers) {
                List<BroadcastReceiver> broadcastReceivers = mReceivers.get(packageName);
                if (broadcastReceivers != null) {
                    Slog.d(TAG, "unregisterReceiver Package: " + packageName + ", size: " + broadcastReceivers.size());
                    for (BroadcastReceiver broadcastReceiver : broadcastReceivers) {
                        try {
                            PrismSpaceCore.getContext().unregisterReceiver(broadcastReceiver);
                        } catch (Throwable ignored) {
                        }
                    }
                }
                mReceivers.remove(packageName);
            }
        }
    }

    @Override
    public void onPackageInstalled(String packageName, int userId) {
        synchronized (mReceivers) {
            mReceivers.remove(packageName);
            PPackageSettings bPackageSetting = mPms.getBPackageSetting(packageName);
            if (bPackageSetting != null) {
                registerPackage(bPackageSetting.pkg);
            }
        }
    }
}

