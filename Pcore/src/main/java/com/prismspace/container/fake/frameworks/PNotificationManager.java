package com.prismspace.container.fake.frameworks;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

import com.prismspace.container.app.PActivityThread;
import com.prismspace.container.core.system.ServiceManager;
import com.prismspace.container.core.system.notification.IBNotificationManagerService;


public class PNotificationManager extends PlackManager<IBNotificationManagerService> {
    private static final PNotificationManager sNotificationManager = new PNotificationManager();

    public static PNotificationManager get() {
        return sNotificationManager;
    }

    @Override
    protected String getServiceName() {
        return ServiceManager.NOTIFICATION_MANAGER;
    }

    public NotificationChannel getNotificationChannel(String channelId) {
        try {
            return getService().getNotificationChannel(channelId, PActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<NotificationChannelGroup> getNotificationChannelGroups(String packageName) {
        try {
            return getService().getNotificationChannelGroups(packageName, PActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void createNotificationChannel(NotificationChannel notificationChannel) {
        try {
            getService().createNotificationChannel(notificationChannel, PActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void deleteNotificationChannel(String channelId) {
        try {
            getService().deleteNotificationChannel(channelId, PActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void createNotificationChannelGroup(NotificationChannelGroup notificationChannelGroup) {
        try {
            getService().createNotificationChannelGroup(notificationChannelGroup, PActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void deleteNotificationChannelGroup(String groupId) {
        try {
            getService().deleteNotificationChannelGroup(groupId, PActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void enqueueNotificationWithTag(int id, String tag, Notification notification) {
        try {
            getService().enqueueNotificationWithTag(id, tag, notification, PActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void cancelNotificationWithTag(int id, String tag) {
        try {
            getService().cancelNotificationWithTag(id, tag, PActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public List<NotificationChannel> getNotificationChannels(String packageName) {
        try {
            return getService().getNotificationChannels(packageName, PActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}

