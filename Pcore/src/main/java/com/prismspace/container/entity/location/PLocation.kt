package com.prismspace.container.entity.location

import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

class PLocation() : Parcelable {
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var altitude: Double = 0.0
    var speed: Float = 0.0f
    var bearing: Float = 0.0f
    var accuracy: Float = 0.0f

    constructor(latitude: Double, longitude: Double) : this() {
        this.latitude = latitude
        this.longitude = longitude
    }

    constructor(parcel: Parcel) : this() {
        latitude = parcel.readDouble()
        longitude = parcel.readDouble()
        altitude = parcel.readDouble()
        // Preserve historical parcel read order for IPC compatibility.
        accuracy = parcel.readFloat()
        speed = parcel.readFloat()
        bearing = parcel.readFloat()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeDouble(latitude)
        dest.writeDouble(longitude)
        dest.writeDouble(altitude)
        dest.writeFloat(speed)
        dest.writeFloat(bearing)
        dest.writeFloat(accuracy)
    }

    fun isEmpty(): Boolean = latitude == 0.0 && longitude == 0.0

    override fun toString(): String {
        return "PLocation{" +
            "latitude: " + latitude +
            ", longitude: " + longitude +
            ", altitude: " + altitude +
            ", speed: " + speed +
            ", bearing: " + bearing +
            ", accuracy: " + accuracy +
            '}'
    }

    fun convert2SystemLocation(): Location {
        val location = Location(LocationManager.GPS_PROVIDER)
        location.latitude = latitude
        location.longitude = longitude
        location.speed = speed
        location.bearing = bearing
        location.accuracy = 40f
        location.time = System.currentTimeMillis()
        val extraBundle = Bundle()
        val satelliteCount = 10
        extraBundle.putInt("satellites", satelliteCount)
        extraBundle.putInt("satellitesvalue", satelliteCount)
        location.extras = extraBundle
        return location
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PLocation> = object : Parcelable.Creator<PLocation> {
            override fun createFromParcel(source: Parcel): PLocation = PLocation(source)

            override fun newArray(size: Int): Array<PLocation?> = arrayOfNulls(size)
        }
    }
}

