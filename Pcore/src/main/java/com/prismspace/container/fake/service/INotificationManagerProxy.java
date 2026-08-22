package com.prismspace.container.fake.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.lang.reflect.Method;
import java.util.List;

import black.android.app.BRNotificationManager;
import black.android.content.pm.BRParceledListSlice;
import com.prismspace.container.app.PActivityThread;
import com.prismspace.container.fake.frameworks.PNotificationManager;
import com.prismspace.container.fake.hook.PinderInvocationStub;
import com.prismspace.container.fake.hook.MethodHook;
import com.prismspace.container.fake.hook.ProxyMethod;
import com.prismspace.container.utils.MethodParameterUtils;
import com.prismspace.container.utils.compat.PuildCompat;
import com.prismspace.container.utils.compat.ParceledListSliceCompat;


public class INotificationManagerProxy extends PinderInvocationStub {
    public static final String TAG = "INotificationManagerProxy";

    public INotificationManagerProxy() {
        super(BRNotificationManager.get().getService().asBinder());
    }

    @Override
    protected Object getWho() {
        return BRNotificationManager.get().getService();
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        BRNotificationManager.get()._set_sService(getProxyInvocation());
        replaceSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        MethodParameterUtils.replaceAllAppPkg(args);
        return super.invoke(proxy, method, args);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getNotificationChannel")
    public static class GetNotificationChannel extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            NotificationChannel notificationChannel = PNotificationManager.get().getNotificationChannel((String) args[args.length - 1]);
            return notificationChannel;
        }
    }

    @ProxyMethod("getNotificationChannels")
    public static class GetNotificationChannels extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            List<NotificationChannel> notificationChannels = PNotificationManager.get().getNotificationChannels(PActivityThread.getAppPackageName());
            return ParceledListSliceCompat.create(notificationChannels);
        }
    }

    @ProxyMethod("cancelNotificationWithTag")
    public static class CancelNotificationWithTag extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String tag = (String) args[getTagIndex()];
            int id = (int) args[getIdIndex()];
            PNotificationManager.get().cancelNotificationWithTag(id, tag);
            return 0;
        }

        public int getTagIndex() {
            if (PuildCompat.isR()) {
                return 2;
            }
            return 1;
        }

        public int getIdIndex() {
            return getTagIndex() + 1;
        }
    }


    @ProxyMethod("enqueueNotificationWithTag")
    public static class EnqueueNotificationWithTag extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String tag = (String) args[getTagIndex()];
            int id = (int) args[getIdIndex()];
            Notification notification = MethodParameterUtils.getFirstParam(args, Notification.class);
            PNotificationManager.get().enqueueNotificationWithTag(id, tag, notification);
            return 0;
        }

        public int getTagIndex() {
            return 2;
        }

        public int getIdIndex() {
            return getTagIndex() + 1;
        }
    }

    @ProxyMethod("createNotificationChannels")
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static class CreateNotificationChannels extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            List<?> list = BRParceledListSlice.get(args[1]).getList();
            if (list == null)
                return 0;
            for (Object o : list) {
                PNotificationManager.get().createNotificationChannel((NotificationChannel) o);
            }
            return 0;
        }
    }

    @ProxyMethod("deleteNotificationChannel")
    public static class DeleteNotificationChannel extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            PNotificationManager.get().deleteNotificationChannel((String) args[1]);
            return 0;
        }
    }

    @ProxyMethod("createNotificationChannelGroups")
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static class CreateNotificationChannelGroups extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            List<?> list = BRParceledListSlice.get(args[1]).getList();
            for (Object o : list) {
                PNotificationManager.get().createNotificationChannelGroup((NotificationChannelGroup) o);
            }
            return 0;
        }
    }

    @ProxyMethod("deleteNotificationChannelGroup")
    public static class DeleteNotificationChannelGroup extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            PNotificationManager.get().deleteNotificationChannelGroup((String) args[1]);
            return 0;
        }
    }

    @ProxyMethod("getNotificationChannelGroups")
    public static class GetNotificationChannelGroups extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            List<NotificationChannelGroup> notificationChannelGroups = PNotificationManager.get().getNotificationChannelGroups(PActivityThread.getAppPackageName());
            return ParceledListSliceCompat.create(notificationChannelGroups);
        }
    }
}

