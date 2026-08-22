package com.prismspace.container.core.system.am;

import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.RemoteException;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

import black.android.app.job.BRJobInfo;
import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.core.system.PProcessManagerService;
import com.prismspace.container.core.system.ISystemService;
import com.prismspace.container.core.system.ProcessRecord;
import com.prismspace.container.core.system.pm.PPackageManagerService;
import com.prismspace.container.entity.JobRecord;
import com.prismspace.container.proxy.ProxyManifest;


public class PJobManagerService extends IBJobManagerService.Stub implements ISystemService {
    private static final PJobManagerService sService = new PJobManagerService();

    
    private final Map<String, JobRecord> mJobRecords = new HashMap<>();

    public static PJobManagerService get() {
        return sService;
    }

    @Override
    public JobInfo schedule(JobInfo info, int userId) throws RemoteException {
        ComponentName componentName = info.getService();
        Intent intent = new Intent();
        intent.setComponent(componentName);
        ResolveInfo resolveInfo = PPackageManagerService.get().resolveService(intent, PackageManager.GET_META_DATA, null, userId);
        if (resolveInfo == null) {
            return info;
        }
        ServiceInfo serviceInfo = resolveInfo.serviceInfo;
        ProcessRecord processRecord = PProcessManagerService.get().findProcessRecord(serviceInfo.packageName, serviceInfo.processName, userId);
        if (processRecord == null) {
            processRecord = PProcessManagerService.get().
                    startProcessLocked(serviceInfo.packageName, serviceInfo.processName, userId, -1, Binder.getCallingPid());
            if (processRecord == null) {
                throw new RuntimeException(
                        "Unable to create Process " + serviceInfo.processName);
            }
        }
        return scheduleJob(processRecord, info, serviceInfo);
    }

    @Override
    public JobRecord queryJobRecord(String processName, int jobId, int userId) throws RemoteException {
        return mJobRecords.get(formatKey(processName, jobId));
    }

    public JobInfo scheduleJob(ProcessRecord processRecord, JobInfo info, ServiceInfo serviceInfo) {
        JobRecord jobRecord = new JobRecord();
        jobRecord.mJobInfo = info;
        jobRecord.mServiceInfo = serviceInfo;

        mJobRecords.put(formatKey(processRecord.processName, info.getId()), jobRecord);
        BRJobInfo.get(info)._set_service(new ComponentName(PrismSpaceCore.getHostPkg(), ProxyManifest.getProxyJobService(processRecord.bpid)));
        return info;
    }

    @Override
    public void cancelAll(String processName, int userId) throws RemoteException {
        if (TextUtils.isEmpty(processName)) return;
        for (String key : mJobRecords.keySet()) {
            if (key.startsWith(processName + "_")) {
                JobRecord jobRecord = mJobRecords.get(key);
                
            }
        }
    }

    @Override
    public int cancel(String processName, int jobId, int userId) throws RemoteException {
        return jobId;
    }

    private String formatKey(String processName, int jobId) {
        return processName + "_" + jobId;
    }

    @Override
    public void systemReady() {

    }
}

