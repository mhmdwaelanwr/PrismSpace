package com.prismspace.container.app.dispatcher

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.res.Configuration
import com.prismspace.container.PrismSpaceCore
import com.prismspace.container.entity.JobRecord
import java.util.HashMap

class AppJobServiceDispatcher private constructor() {
    private val mJobRecords: MutableMap<Int, JobRecord> = HashMap()

    fun onStartJob(params: JobParameters?): Boolean {
        if (params == null) {
            return false
        }
        return try {
            val jobService = getJobService(params.jobId) ?: return false
            jobService.onStartJob(params)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun onStopJob(params: JobParameters?): Boolean {
        if (params == null) {
            return false
        }
        val jobService = getJobService(params.jobId) ?: return false
        val shouldRetry = jobService.onStopJob(params)
        jobService.onDestroy()
        synchronized(mJobRecords) {
            mJobRecords.remove(params.jobId)
        }
        return shouldRetry
    }

    fun onConfigurationChanged(newConfig: Configuration?) {
        if (newConfig == null) {
            return
        }
        for (jobRecord in mJobRecords.values) {
            val jobService = jobRecord.mJobService
            if (jobService != null) {
                jobService.onConfigurationChanged(newConfig)
            }
        }
    }

    fun onDestroy() {
    }

    fun onLowMemory() {
        for (jobRecord in mJobRecords.values) {
            val jobService = jobRecord.mJobService
            if (jobService != null) {
                jobService.onLowMemory()
            }
        }
    }

    fun onTrimMemory(level: Int) {
        for (jobRecord in mJobRecords.values) {
            val jobService = jobRecord.mJobService
            if (jobService != null) {
                jobService.onTrimMemory(level)
            }
        }
    }

    fun getJobService(jobId: Int): JobService? {
        synchronized(mJobRecords) {
            val cachedRecord = mJobRecords[jobId]
            if (cachedRecord?.mJobService != null) {
                return cachedRecord.mJobService
            }
            try {
                val record = PrismSpaceCore.getBJobManager().queryJobRecord(PrismSpaceCore.getAppProcessName(), jobId)
                    ?: return null
                record.mJobService = PrismSpaceCore.currentActivityThread().createJobService(record.mServiceInfo)
                if (record.mJobService == null) {
                    return null
                }
                mJobRecords[jobId] = record
                return record.mJobService
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            return null
        }
    }

    companion object {
        @JvmField
        val sServiceDispatcher: AppJobServiceDispatcher = AppJobServiceDispatcher()

        @JvmStatic
        fun get(): AppJobServiceDispatcher = sServiceDispatcher
    }
}

