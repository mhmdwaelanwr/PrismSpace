package com.prismspace.container.core.system;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.core.IBActivityThread;
import com.prismspace.container.core.env.PEnvironment;
import com.prismspace.container.core.system.notification.PNotificationManagerService;
import com.prismspace.container.core.system.pm.PPackageManagerService;
import com.prismspace.container.core.system.user.PUserHandle;
import com.prismspace.container.entity.AppConfig;
import com.prismspace.container.proxy.ProxyManifest;
import com.prismspace.container.utils.FileUtils;
import com.prismspace.container.utils.Slog;
import com.prismspace.container.utils.compat.ApplicationThreadCompat;
import com.prismspace.container.utils.compat.PundleCompat;
import com.prismspace.container.utils.provider.ProviderCall;


public class PProcessManagerService implements ISystemService {
    public static final String TAG = "BProcessManager";

    public static PProcessManagerService sBProcessManagerService = new PProcessManagerService();
    private final Map<Integer, Map<String, ProcessRecord>> mProcessMap = new HashMap<>();
    private final List<ProcessRecord> mPidsSelfLocked = new ArrayList<>();
    private final Object mProcessLock = new Object();

    public static PProcessManagerService get() {
        return sBProcessManagerService;
    }

    public ProcessRecord startProcessLocked(String packageName, String processName, int userId, int bpid, int callingPid) {
        ProcessRecord app;
        int appId;
        int buid;
        synchronized (mProcessLock) {
            appId = PPackageManagerService.get().getAppId(packageName);
            buid = PUserHandle.getUid(userId, appId);
            Map<String, ProcessRecord> bProcess = mProcessMap.get(buid);

            if (bProcess == null) {
                bProcess = new HashMap<>();
            }
            if (bpid == -1) {
                app = bProcess.get(processName);
                if (app != null) {
                    if (app.initLock != null) {
                        app.initLock.block();
                    }
                    if (app.bActivityThread != null) {
                        return app;
                    }
                }
                bpid = getUsingBPidL();
                Slog.d(TAG, "init bUid = " + buid + ", bPid = " + bpid);
            }
            if (bpid == -1) {
                throw new RuntimeException("No processes available");
            }
            ApplicationInfo info = PPackageManagerService.get().getApplicationInfo(packageName, 0, userId);
            if (info == null) {
                return null;
            }
            app = new ProcessRecord(info, processName);
            app.uid = Process.myUid();
            app.bpid = bpid;
            app.buid = appId;
            app.callingBUid = getBUidByPidOrPackageName(callingPid, packageName);
            app.userId = userId;

            bProcess.put(processName, app);
            mPidsSelfLocked.add(app);

            mProcessMap.put(buid, bProcess);
            if (!initAppProcessL(app)) {
                
                bProcess.remove(processName);
                mPidsSelfLocked.remove(app);
                app = null;
            } else {
                app.pid = getPid(PrismSpaceCore.getContext(), ProxyManifest.getProcessName(app.bpid));
            }
        }
        return app;
    }

    private int getUsingBPidL() {
        ActivityManager manager = (ActivityManager) PrismSpaceCore.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = manager.getRunningAppProcesses();
        Set<Integer> usingPs = new HashSet<>();
        for (ActivityManager.RunningAppProcessInfo runningAppProcess : runningAppProcesses) {
            int i = parseBPid(runningAppProcess.processName);
            usingPs.add(i);
        }
        for (int i = 0; i < ProxyManifest.FREE_COUNT; i++) {
            if (usingPs.contains(i)) {
                continue;
            }
            return i;
        }
        return -1;
    }

    public void restartAppProcess(String packageName, String processName, int userId) {
        if (PrismSpaceCore.get().isPackageRestartSuppressed(packageName, userId)) {
            Slog.d(TAG, "restartAppProcess suppressed for package: " + packageName + " userId=" + userId);
            return;
        }
        synchronized (mProcessLock) {
            int callingPid = Binder.getCallingPid();
            ProcessRecord app = findProcessByPid(callingPid);
            if (app == null) {
                String stubProcessName = getProcessName(PrismSpaceCore.getContext(), callingPid);
                int bpid = parseBPid(stubProcessName);
                startProcessLocked(packageName, processName, userId, bpid, callingPid);
            }
        }
    }

    private int parseBPid(String stubProcessName) {
        String prefix;
        if (stubProcessName == null) {
            return -1;
        } else {
            prefix = PrismSpaceCore.getHostPkg() + ":p";
        }
        if (stubProcessName.startsWith(prefix)) {
            try {
                return Integer.parseInt(stubProcessName.substring(prefix.length()));
            } catch (NumberFormatException e) {
                
            }
        }
        return -1;
    }

    private boolean initAppProcessL(ProcessRecord record) {
        Log.d(TAG, "initProcess: " + record.processName);
        AppConfig appConfig = record.getClientConfig();
        Bundle bundle = new Bundle();
        bundle.putParcelable(AppConfig.KEY, appConfig);
        Bundle init = ProviderCall.callSafely(record.getProviderAuthority(), "_Black_|_init_process_", null, bundle);
        IBinder appThread = PundleCompat.getBinder(init, "_Black_|_client_");
        if (appThread == null || !appThread.isBinderAlive()) {
            return false;
        }
        attachClientL(record, appThread);

        createProc(record);
        return true;
    }

    private void attachClientL(final ProcessRecord app, final IBinder appThread) {
        IBActivityThread activityThread = IBActivityThread.Stub.asInterface(appThread);
        if (activityThread == null) {
            app.kill();
            return;
        }
        try {
            appThread.linkToDeath(new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    Log.d(TAG, "App Died: " + app.processName);
                    appThread.unlinkToDeath(this, 0);
                    onProcessDie(app);
                }
            }, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        app.bActivityThread = activityThread;
        try {
            app.appThread = ApplicationThreadCompat.asInterface(activityThread.getActivityThread());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        app.initLock.open();
    }

    public void onProcessDie(ProcessRecord record) {
        synchronized (mProcessLock) {
            record.kill();
            Map<String, ProcessRecord> process = mProcessMap.get(record.buid);
            if (process != null) {
                process.remove(record.processName);
                if (process.isEmpty()) {
                    mProcessMap.remove(record.buid);
                }
            }
            mPidsSelfLocked.remove(record);

            removeProc(record);
            PNotificationManagerService.get().deletePackageNotification(record.getPackageName(), record.userId);
        }
    }

    public ProcessRecord findProcessRecord(String packageName, String processName, int userId) {
        synchronized (mProcessMap) {
            int appId = PPackageManagerService.get().getAppId(packageName);
            int buid = PUserHandle.getUid(userId, appId);
            Map<String, ProcessRecord> processRecordMap = mProcessMap.get(buid);
            if (processRecordMap == null)
                return null;
            return processRecordMap.get(processName);
        }
    }

    public void killAllByPackageName(String packageName) {
        synchronized (mProcessLock) {
            synchronized (mPidsSelfLocked) {
                List<ProcessRecord> tmp = new ArrayList<>(mPidsSelfLocked);
                int appId = PPackageManagerService.get().getAppId(packageName);
                for (ProcessRecord processRecord : mPidsSelfLocked) {
                    int appId1 = PUserHandle.getAppId(processRecord.buid);
                    if (appId == appId1) {
                        mProcessMap.remove(processRecord.buid);
                        tmp.remove(processRecord);
                        processRecord.kill();
                    }
                }
                mPidsSelfLocked.clear();
                mPidsSelfLocked.addAll(tmp);
            }
        }
    }

    public void killPackageAsUser(String packageName, int userId) {
        synchronized (mProcessLock) {
            int buid = PUserHandle.getUid(userId, PPackageManagerService.get().getAppId(packageName));
            Map<String, ProcessRecord> process = mProcessMap.get(buid);
            if (process == null)
                return;
            for (ProcessRecord value : process.values()) {
                value.kill();
                mPidsSelfLocked.remove(value);
            }
            mProcessMap.remove(buid);
        }
    }

    public List<ProcessRecord> getPackageProcessAsUser(String packageName, int userId) {
        synchronized (mProcessMap) {
            int buid = PUserHandle.getUid(userId, PPackageManagerService.get().getAppId(packageName));
            Map<String, ProcessRecord> process = mProcessMap.get(buid);
            if (process == null)
                return new ArrayList<>();
            return new ArrayList<>(process.values());
        }
    }

    public int getBUidByPidOrPackageName(int pid, String packageName) {
        ProcessRecord callingProcess = findProcessByPid(pid);
        if (callingProcess == null) {
            return PPackageManagerService.get().getAppId(packageName);
        }
        return PUserHandle.getAppId(callingProcess.buid);
    }

    public int getUserIdByCallingPid(int callingPid) {
        ProcessRecord callingProcess = findProcessByPid(callingPid);
        if (callingProcess == null) {
            return 0;
        }
        return callingProcess.userId;
    }

    public ProcessRecord findProcessByPid(int pid) {
        synchronized (mPidsSelfLocked) {
            for (ProcessRecord processRecord : mPidsSelfLocked) {
                if (processRecord.pid == pid)
                    return processRecord;
            }
            return null;
        }
    }

    private static String getProcessName(Context context, int pid) {
        String processName = null;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
            if (info.pid == pid) {
                processName = info.processName;
                break;
            }
        }
        if (processName == null) {
            throw new RuntimeException("processName = null");
        }
        return processName;
    }

    public static int getPid(Context context, String processName) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = manager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo runningAppProcess : runningAppProcesses) {
                if (runningAppProcess.processName.equals(processName)) {
                    return runningAppProcess.pid;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static void createProc(ProcessRecord record) {
        File cmdline = new File(PEnvironment.getProcDir(record.bpid), "cmdline");
        try {
            FileUtils.writeToFile(record.processName.getBytes(), cmdline);
        } catch (IOException ignored) {
        }
    }

    private static void removeProc(ProcessRecord record) {
        FileUtils.deleteDir(PEnvironment.getProcDir(record.bpid));
    }

    @Override
    public void systemReady() {
        FileUtils.deleteDir(PEnvironment.getProcDir());
    }
}

