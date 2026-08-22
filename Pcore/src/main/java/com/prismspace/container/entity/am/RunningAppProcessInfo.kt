package com.prismspace.container.entity.am

import android.app.ActivityManager
import android.os.Parcel
import android.os.Parcelable
import java.util.ArrayList

class RunningAppProcessInfo() : Parcelable {
    @JvmField
    var mAppProcessInfoList: MutableList<ActivityManager.RunningAppProcessInfo> = ArrayList()

    private constructor(parcel: Parcel) : this() {
        mAppProcessInfoList = parcel.createTypedArrayList(ActivityManager.RunningAppProcessInfo.CREATOR) ?: ArrayList()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedList(mAppProcessInfoList)
    }

    fun readFromParcel(source: Parcel) {
        mAppProcessInfoList = source.createTypedArrayList(ActivityManager.RunningAppProcessInfo.CREATOR) ?: ArrayList()
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<RunningAppProcessInfo> = object : Parcelable.Creator<RunningAppProcessInfo> {
            override fun createFromParcel(source: Parcel): RunningAppProcessInfo = RunningAppProcessInfo(source)

            override fun newArray(size: Int): Array<RunningAppProcessInfo?> = arrayOfNulls(size)
        }
    }
}

