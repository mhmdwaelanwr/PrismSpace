package com.prismspace.container.entity

import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable

class AppConfig() : Parcelable {
    @JvmField
    var packageName: String? = null

    @JvmField
    var processName: String? = null

    @JvmField
    var bpid: Int = 0

    @JvmField
    var buid: Int = 0

    @JvmField
    var uid: Int = 0

    @JvmField
    var userId: Int = 0

    @JvmField
    var callingBUid: Int = 0

    @JvmField
    var token: IBinder? = null

    private constructor(parcel: Parcel) : this() {
        packageName = parcel.readString()
        processName = parcel.readString()
        bpid = parcel.readInt()
        buid = parcel.readInt()
        uid = parcel.readInt()
        userId = parcel.readInt()
        callingBUid = parcel.readInt()
        token = parcel.readStrongBinder()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(packageName)
        dest.writeString(processName)
        dest.writeInt(bpid)
        dest.writeInt(buid)
        dest.writeInt(uid)
        dest.writeInt(userId)
        dest.writeInt(callingBUid)
        dest.writeStrongBinder(token)
    }

    companion object {
        @JvmField
        val KEY: String = "PrismSpace_client_config"

        @JvmField
        val CREATOR: Parcelable.Creator<AppConfig> = object : Parcelable.Creator<AppConfig> {
            override fun createFromParcel(source: Parcel): AppConfig = AppConfig(source)

            override fun newArray(size: Int): Array<AppConfig?> = arrayOfNulls(size)
        }
    }
}

