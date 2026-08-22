package com.prismspace.container.core.system.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.prismspace.container.PrismSpaceCore;


public class NotificationChannelManager {
    private final static NotificationChannelManager sManager = new NotificationChannelManager();

    public static NotificationChannel APP_CHANNEL;

    public static NotificationChannelManager get() {
        return sManager;
    }

    public NotificationChannelManager() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerAppChannel();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void registerAppChannel() {
        NotificationManager nm = (NotificationManager) PrismSpaceCore.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String CHANNEL_ONE_ID = PrismSpaceCore.getContext().getPackageName();
        String CHANNEL_ONE_NAME = "black-box-app";
        APP_CHANNEL = new NotificationChannel(CHANNEL_ONE_ID,
                CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH);
        APP_CHANNEL.enableLights(true);
        APP_CHANNEL.setLightColor(Color.RED);
        APP_CHANNEL.setShowBadge(true);
        APP_CHANNEL.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(APP_CHANNEL);
    }
}

