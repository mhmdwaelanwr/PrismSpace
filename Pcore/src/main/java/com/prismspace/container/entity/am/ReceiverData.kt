package com.prismspace.container.entity.am

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Parcel
import android.os.Parcelable

class ReceiverData() : Parcelable {
    @JvmField
    var intent: Intent? = null

    @JvmField
    var activityInfo: ActivityInfo? = null

    @JvmField
    var data: PendingResultData? = null

    private constructor(parcel: Parcel) : this() {
        intent = parcel.readParcelable(Intent::class.java.classLoader)
        activityInfo = parcel.readParcelable(ActivityInfo::class.java.classLoader)
        data = parcel.readParcelable(PendingResultData::class.java.classLoader)
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(intent, flags)
        dest.writeParcelable(activityInfo, flags)
        dest.writeParcelable(data, flags)
    }

    fun readFromParcel(source: Parcel) {
        intent = source.readParcelable(Intent::class.java.classLoader)
        activityInfo = source.readParcelable(ActivityInfo::class.java.classLoader)
        data = source.readParcelable(PendingResultData::class.java.classLoader)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ReceiverData> = object : Parcelable.Creator<ReceiverData> {
            override fun createFromParcel(source: Parcel): ReceiverData = ReceiverData(source)

            override fun newArray(size: Int): Array<ReceiverData?> = arrayOfNulls(size)
        }
    }
}

