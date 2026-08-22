package com.prismspace.container.entity.location

import android.os.Parcel
import android.os.Parcelable

class PCell() : Parcelable {
    @JvmField
    var MCC = 0

    @JvmField
    var MNC = 0

    @JvmField
    var LAC = 0

    @JvmField
    var CID = 0

    @JvmField
    var TYPE = PHONE_TYPE_GSM

    constructor(MCC: Int, MNC: Int, LAC: Int, CID: Int) : this() {
        TYPE = PHONE_TYPE_GSM
        this.MCC = MCC
        this.CID = CID
        this.MNC = MNC
        this.LAC = LAC
    }

    private constructor(parcel: Parcel) : this() {
        MCC = parcel.readInt()
        MNC = parcel.readInt()
        LAC = parcel.readInt()
        CID = parcel.readInt()
        TYPE = parcel.readInt()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(MCC)
        dest.writeInt(MNC)
        dest.writeInt(LAC)
        dest.writeInt(CID)
        dest.writeInt(TYPE)
    }

    companion object {
        @JvmField
        val NETWORK_TYPE_UNKNOWN = 0

        @JvmField
        val NETWORK_TYPE_GPRS = 1

        @JvmField
        val NETWORK_TYPE_EDGE = 2

        @JvmField
        val NETWORK_TYPE_UMTS = 3

        @JvmField
        val NETWORK_TYPE_CDMA = 4

        @JvmField
        val NETWORK_TYPE_EVDO_0 = 5

        @JvmField
        val NETWORK_TYPE_EVDO_A = 6

        @JvmField
        val NETWORK_TYPE_1xRTT = 7

        @JvmField
        val PHONE_TYPE_NONE = 0

        @JvmField
        val PHONE_TYPE_GSM = 1

        @JvmField
        val PHONE_TYPE_CDMA = 2

        @JvmField
        val CREATOR: Parcelable.Creator<PCell> = object : Parcelable.Creator<PCell> {
            override fun createFromParcel(source: Parcel): PCell = PCell(source)

            override fun newArray(size: Int): Array<PCell?> = arrayOfNulls(size)
        }
    }
}

