package com.prismspace.container.entity.pm

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Parcel
import android.os.Parcelable
import com.prismspace.container.PrismSpaceCore
import java.util.Objects

class InstalledPackage() : Parcelable {
    @JvmField
    var userId = 0

    @JvmField
    var packageName: String? = null

    constructor(packageName: String?) : this() {
        this.packageName = packageName
    }

    private constructor(parcel: Parcel) : this() {
        userId = parcel.readInt()
        packageName = parcel.readString()
    }

    fun getApplication(): ApplicationInfo? {
        return PrismSpaceCore.getBPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA, userId)
    }

    fun getPackageInfo(): PackageInfo? {
        return PrismSpaceCore.getBPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA, userId)
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(userId)
        dest.writeString(packageName)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as InstalledPackage
        return Objects.equals(packageName, other.packageName)
    }

    override fun hashCode(): Int {
        return Objects.hash(packageName)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<InstalledPackage> = object : Parcelable.Creator<InstalledPackage> {
            override fun createFromParcel(source: Parcel): InstalledPackage = InstalledPackage(source)

            override fun newArray(size: Int): Array<InstalledPackage?> = arrayOfNulls(size)
        }
    }
}

