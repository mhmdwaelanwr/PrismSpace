package com.prismspace.container.entity.am

import android.app.ActivityManager
import android.os.Parcel
import android.os.Parcelable
import java.util.ArrayList

class RunningServiceInfo() : Parcelable {
    @JvmField
    var mRunningServiceInfoList: MutableList<ActivityManager.RunningServiceInfo> = ArrayList()

    private constructor(parcel: Parcel) : this() {
        mRunningServiceInfoList = parcel.createTypedArrayList(ActivityManager.RunningServiceInfo.CREATOR) ?: ArrayList()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedList(mRunningServiceInfoList)
    }

    fun readFromParcel(source: Parcel) {
        mRunningServiceInfoList = source.createTypedArrayList(ActivityManager.RunningServiceInfo.CREATOR) ?: ArrayList()
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<RunningServiceInfo> = object : Parcelable.Creator<RunningServiceInfo> {
            override fun createFromParcel(source: Parcel): RunningServiceInfo = RunningServiceInfo(source)

            override fun newArray(size: Int): Array<RunningServiceInfo?> = arrayOfNulls(size)
        }
    }
}

