package com.prismspace.container.entity.pm

import android.os.Parcel
import android.os.Parcelable
import com.prismspace.container.utils.Slog

class InstallResult() : Parcelable {
    @JvmField
    var success = true

    @JvmField
    var packageName: String? = null

    @JvmField
    var msg: String? = null

    private constructor(parcel: Parcel) : this() {
        success = parcel.readByte().toInt() != 0
        packageName = parcel.readString()
        msg = parcel.readString()
    }

    fun installError(packageName: String?, msg: String?): InstallResult {
        this.msg = msg
        success = false
        this.packageName = packageName
        Slog.d(TAG, msg)
        return this
    }

    fun installError(msg: String?): InstallResult {
        this.msg = msg
        success = false
        Slog.d(TAG, msg)
        return this
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte(if (success) 1 else 0)
        dest.writeString(packageName)
        dest.writeString(msg)
    }

    companion object {
        @JvmField
        val TAG = "InstallResult"

        @JvmField
        val CREATOR: Parcelable.Creator<InstallResult> = object : Parcelable.Creator<InstallResult> {
            override fun createFromParcel(source: Parcel): InstallResult = InstallResult(source)

            override fun newArray(size: Int): Array<InstallResult?> = arrayOfNulls(size)
        }
    }
}

