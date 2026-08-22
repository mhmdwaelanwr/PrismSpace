package com.prismspace.container.core.system;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


public class DaemonService extends Service {
    public static final String TAG = "DaemonService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "DaemonService compatibility shell active");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "DaemonService start ignored (deprecated)");
        stopSelfResult(startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "DaemonService onDestroy");
        super.onDestroy();
    }

    public static class DaemonInnerService extends Service {
        @Override
        public void onCreate() {
            super.onCreate();
            Log.d(TAG, "DaemonInnerService compatibility shell active");
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.d(TAG, "DaemonInnerService start ignored (deprecated)");
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onDestroy() {
            Log.d(TAG, "DaemonInnerService onDestroy");
            super.onDestroy();
        }
    }
}

