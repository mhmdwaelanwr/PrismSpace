package com.prismspace.container.core;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * H6: IPC Service for Policy Distribution.
 * Uses Messenger to provide the current policy PFD to all client processes.
 */
public class ClonePolicyService extends Service {
    private static final String TAG = "ClonePolicyService";

    public static final int MSG_GET_CURRENT_POLICY = 100;
    public static final String KEY_POLICY_PFD = "policy_pfd";
    public static final String KEY_POLICY_SIZE = "policy_size";
    public static final String KEY_POLICY_GEN = "policy_gen";

    private final Messenger mMessenger = new Messenger(new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_GET_CURRENT_POLICY) {
                replyWithCurrentPolicy(msg.replyTo);
            } else {
                super.handleMessage(msg);
            }
        }
    });

    private void replyWithCurrentPolicy(Messenger client) {
        if (client == null) return;

        PolicyPublisher.PublishedBlob current = IOCore.get().getPolicyPublisher().getCurrentBlob();
        if (current == null) {
            Log.w(TAG, "Client requested policy but none is published yet.");
            return;
        }

        try {
            Message reply = Message.obtain(null, MSG_GET_CURRENT_POLICY);
            Bundle data = new Bundle();
            data.putParcelable(KEY_POLICY_PFD, current.pfd);
            data.putInt(KEY_POLICY_SIZE, current.size);
            data.putLong(KEY_POLICY_GEN, current.generation);
            reply.setData(data);
            client.send(reply);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to send policy to client", e);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
}
