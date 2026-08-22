package com.prismspace.container.entity.location

import android.os.Parcel
import android.os.Parcelable

class PLocationConfig() : Parcelable {
    @JvmField
    var pattern = 0

    @JvmField
    var cell: PCell? = null

    @JvmField
    var allCell: MutableList<PCell>? = null

    @JvmField
    var neighboringCellInfo: MutableList<PCell>? = null

    @JvmField
    var location: PLocation? = null

    constructor(parcel: Parcel) : this() {
        refresh(parcel)
    }

    fun refresh(parcel: Parcel) {
        pattern = parcel.readInt()
        cell = parcel.readParcelable(PCell::class.java.classLoader)
        allCell = parcel.createTypedArrayList(PCell.CREATOR)
        neighboringCellInfo = parcel.createTypedArrayList(PCell.CREATOR)
        location = parcel.readParcelable(PLocation::class.java.classLoader)
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(pattern)
        dest.writeParcelable(cell, flags)
        dest.writeTypedList(allCell)
        dest.writeTypedList(neighboringCellInfo)
        dest.writeParcelable(location, flags)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PLocationConfig> = object : Parcelable.Creator<PLocationConfig> {
            override fun createFromParcel(source: Parcel): PLocationConfig = PLocationConfig(source)

            override fun newArray(size: Int): Array<PLocationConfig?> = arrayOfNulls(size)
        }
    }
}

