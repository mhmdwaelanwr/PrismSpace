package com.prismspace.container.data

import androidx.lifecycle.MutableLiveData
import com.prismspace.container.PrismSpaceCore
import com.prismspace.container.R
import com.prismspace.container.app.AppManager
import com.prismspace.container.bean.GmsBean
import com.prismspace.container.bean.GmsInstallBean
import com.prismspace.container.util.getString


class GmsRepository {


    fun getGmsInstalledList(mInstalledLiveData: MutableLiveData<List<GmsBean>>) {
        val userList = arrayListOf<GmsBean>()

        PrismSpaceCore.get().users.forEach {
            val userId = it.id
            val userName =
                AppManager.mRemarkSharedPreferences.getString("Remark$userId", "User $userId") ?: ""
            val isInstalled = PrismSpaceCore.get().isInstallGms(userId)
            val bean = GmsBean(userId, userName, isInstalled)
            userList.add(bean)
        }

        mInstalledLiveData.postValue(userList)
    }

    fun installGms(
        userID: Int,
        mUpdateInstalledLiveData: MutableLiveData<GmsInstallBean>
    ) {
        val installResult = PrismSpaceCore.get().installGms(userID)

        val result = if (installResult.success) {
            getString(R.string.install_success)
        } else {
            getString(R.string.install_fail, installResult.msg ?: "")
        }

        val bean = GmsInstallBean(userID,installResult.success,result)
        mUpdateInstalledLiveData.postValue(bean)
    }

    fun uninstallGms(
        userID: Int,
        mUpdateInstalledLiveData: MutableLiveData<GmsInstallBean>
    ) {
        var isSuccess = false
        if (PrismSpaceCore.get().isInstallGms(userID)) {
            isSuccess = PrismSpaceCore.get().uninstallGms(userID)
        }

        val result = if (isSuccess) {
            getString(R.string.uninstall_success)
        } else {
            getString(R.string.uninstall_fail)
        }

        val bean = GmsInstallBean(userID,isSuccess,result)

        mUpdateInstalledLiveData.postValue(bean)
    }
}
