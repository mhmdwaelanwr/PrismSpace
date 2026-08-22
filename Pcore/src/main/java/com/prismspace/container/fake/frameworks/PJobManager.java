package com.prismspace.container.fake.frameworks;

import android.app.job.JobInfo;
import android.os.RemoteException;

import com.prismspace.container.app.PActivityThread;
import com.prismspace.container.core.system.ServiceManager;
import com.prismspace.container.core.system.am.IBJobManagerService;
import com.prismspace.container.entity.JobRecord;


public class PJobManager extends PlackManager<IBJobManagerService> {
    private static final PJobManager sJobManager = new PJobManager();

    public static PJobManager get() {
        return sJobManager;
    }

    @Override
    protected String getServiceName() {
        return ServiceManager.JOB_MANAGER;
    }

    public JobInfo schedule(JobInfo info) {
        try {
            return getService().schedule(info, PActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JobRecord queryJobRecord(String processName, int jobId) {
        try {
            return getService().queryJobRecord(processName, jobId, PActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void cancelAll(String processName) {
        try {
            getService().cancelAll(processName, PActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public int cancel(String processName, int jobId) {
        try {
            return getService().cancel(processName, jobId, PActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return -1;
    }
}

