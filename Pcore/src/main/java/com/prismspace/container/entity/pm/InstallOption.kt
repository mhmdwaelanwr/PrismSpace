package com.prismspace.container.entity.pm

import android.os.Parcel
import android.os.Parcelable

class InstallOption() : Parcelable {
    @JvmField
    var flags = 0

    private constructor(parcel: Parcel) : this() {
        flags = parcel.readInt()
    }

    fun makeUriFile(): InstallOption {
        flags = flags or FLAG_URI_FILE
        return this
    }

    fun isFlag(flag: Int): Boolean {
        return (flags and flag) != 0
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(this.flags)
    }

    companion object {
        @JvmField
        val FLAG_SYSTEM = 1

        @JvmField
        val FLAG_STORAGE = 1 shl 1

        @JvmField
        val FLAG_URI_FILE = 1 shl 3

        @JvmStatic
        fun installBySystem(): InstallOption {
            val installOption = InstallOption()
            installOption.flags = installOption.flags or FLAG_SYSTEM
            return installOption
        }

        @JvmStatic
        fun installByStorage(): InstallOption {
            val installOption = InstallOption()
            installOption.flags = installOption.flags or FLAG_STORAGE
            return installOption
        }

        @JvmField
        val CREATOR: Parcelable.Creator<InstallOption> = object : Parcelable.Creator<InstallOption> {
            override fun createFromParcel(source: Parcel): InstallOption = InstallOption(source)

            override fun newArray(size: Int): Array<InstallOption?> = arrayOfNulls(size)
        }
    }
}

