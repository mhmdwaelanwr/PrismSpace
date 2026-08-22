package com.prismspace.container.data

import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.prismspace.container.PrismSpaceCore
import com.prismspace.container.entity.location.PLocation
import com.prismspace.container.fake.frameworks.PLocationManager
import com.prismspace.container.bean.FakeLocationBean


class FakeLocationRepository {
    val TAG: String = "FakeLocationRepository"

    fun setPattern(userId: Int, pkg: String, pattern: Int) {
        PLocationManager.get().setPattern(userId, pkg, pattern)
    }

    private fun getPattern(userId: Int, pkg: String): Int {
        return PLocationManager.get().getPattern(userId, pkg)
    }

    private fun getLocation(userId: Int, pkg: String): PLocation? {
        return PLocationManager.get().getLocation(userId, pkg)
    }

    fun setLocation(userId: Int, pkg: String, location: PLocation) {
        PLocationManager.get().setLocation(userId, pkg, location)
    }

    fun getInstalledAppList(
        userID: Int,
        appsFakeLiveData: MutableLiveData<List<FakeLocationBean>>
    ) {
        val installedList = mutableListOf<FakeLocationBean>()
        val installedApplications: List<ApplicationInfo> =
            PrismSpaceCore.get().getInstalledApplications(0, userID)
        
        for (installedApplication in installedApplications) {








            val info = FakeLocationBean(
                userID,
                installedApplication.loadLabel(PrismSpaceCore.getPackageManager()).toString(),
                installedApplication.loadIcon(PrismSpaceCore.getPackageManager()),
                installedApplication.packageName,
                getPattern(userID, installedApplication.packageName),
                getLocation(userID, installedApplication.packageName)
            )
            installedList.add(info)
        }

        Log.d(TAG, installedList.joinToString(","))
        appsFakeLiveData.postValue(installedList)
    }
}
