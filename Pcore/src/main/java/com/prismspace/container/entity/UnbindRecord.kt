package com.prismspace.container.entity

import android.content.ComponentName
import android.os.Parcel
import android.os.Parcelable

class UnbindRecord() : Parcelable {
    private var mBindCount = 0
    private var mStartId = 0
    private var mComponentName: ComponentName? = null

    fun getStartId(): Int = mStartId

    fun setStartId(startId: Int) {
        mStartId = startId
    }

    fun getBindCount(): Int = mBindCount

    fun setBindCount(bindCount: Int) {
        mBindCount = bindCount
    }

    fun getComponentName(): ComponentName? = mComponentName

    fun setComponentName(componentName: ComponentName?) {
        mComponentName = componentName
    }

    private constructor(parcel: Parcel) : this() {
        mBindCount = parcel.readInt()
        mStartId = parcel.readInt()
        mComponentName = parcel.readParcelable(ComponentName::class.java.classLoader)
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(mBindCount)
        dest.writeInt(mStartId)
        dest.writeParcelable(mComponentName, flags)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<UnbindRecord> = object : Parcelable.Creator<UnbindRecord> {
            override fun createFromParcel(source: Parcel): UnbindRecord = UnbindRecord(source)

            override fun newArray(size: Int): Array<UnbindRecord?> = arrayOfNulls(size)
        }

        @JvmStatic
        fun getCREATOR(): Parcelable.Creator<UnbindRecord> = CREATOR
    }
}

