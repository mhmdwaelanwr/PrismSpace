package com.prismspace.container.entity

import android.app.job.JobInfo
import android.app.job.JobService
import android.content.pm.ServiceInfo
import android.os.Parcel
import android.os.Parcelable

class JobRecord() : Parcelable {
    @JvmField
    var mJobInfo: JobInfo? = null

    @JvmField
    var mServiceInfo: ServiceInfo? = null

    @JvmField
    var mJobService: JobService? = null

    private constructor(parcel: Parcel) : this() {
        mJobInfo = parcel.readParcelable(JobInfo::class.java.classLoader)
        mServiceInfo = parcel.readParcelable(ServiceInfo::class.java.classLoader)
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(mJobInfo, flags)
        dest.writeParcelable(mServiceInfo, flags)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<JobRecord> = object : Parcelable.Creator<JobRecord> {
            override fun createFromParcel(source: Parcel): JobRecord = JobRecord(source)

            override fun newArray(size: Int): Array<JobRecord?> = arrayOfNulls(size)
        }
    }
}

