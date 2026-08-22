package com.prismspace.container.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SharedMemory;
import android.util.Log;

import com.prismspace.container.PrismSpaceCore;

public class ClonePolicyClient {
    private static final String TAG = "ClonePolicyClient";
    private static final ClonePolicyClient sInstance = new ClonePolicyClient();

    private Messenger mService = null;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    private boolean mBound = false;

    public static ClonePolicyClient get() {
        return sInstance;
    }

    public synchronized void bind(Context context) {
        if (mBound || context == null) return;
        Intent intent = new Intent();
        intent.setClassName(context.getPackageName(), "com.prismspace.container.core.ClonePolicyService");
        mBound = context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = new Messenger(service);
            mBound = true;
            requestCurrentPolicy();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    private void requestCurrentPolicy() {
        if (mService == null) return;
        try {
            Message msg = Message.obtain(null, ClonePolicyService.MSG_GET_CURRENT_POLICY);
            msg.replyTo = mMessenger;
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to request current policy", e);
        }
    }

    private static class IncomingHandler extends android.os.Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ClonePolicyService.MSG_GET_CURRENT_POLICY) {
                handleCurrentPolicy(msg);
                return;
            }
            super.handleMessage(msg);
        }

        private void handleCurrentPolicy(Message msg) {
            android.os.Bundle data = msg.getData();
            if (data == null) return;
            data.setClassLoader(ClonePolicyClient.class.getClassLoader());

            ParcelFileDescriptor pfd = data.getParcelable(ClonePolicyService.KEY_POLICY_PFD);
            long size = data.getInt(ClonePolicyService.KEY_POLICY_SIZE, 0);
            long generation = data.getLong(ClonePolicyService.KEY_POLICY_GEN, 0L);

            if (pfd == null || size <= 0L) {
                Log.w(TAG, "Received empty policy artifact");
                return;
            }

            try {
                boolean ok = NativeCore.nativeInstallPolicyFromFd(pfd.getFd(), size, generation, 0);
                if (!ok) {
                    Log.e(TAG, "nativeInstallPolicyFromFd returned false for generation=" + generation);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Failed to install policy in client process", t);
            }
        }
    }
}
