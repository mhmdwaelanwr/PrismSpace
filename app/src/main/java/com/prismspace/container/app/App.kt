package com.prismspace.container.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import com.prismspace.container.PrismSpaceCore
import com.prismspace.container.crash.CrashLogSubmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class App : Application() {

    companion object {

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private lateinit var mContext: Context

        @JvmStatic
        fun getContext(): Context {
            return mContext
        }
    }

    override fun attachBaseContext(base: Context?) {
        try {
            super.attachBaseContext(base)

            try {
                PrismSpaceCore.get().closeCodeInit()
            } catch (e: Exception) {
                Log.e("App", "Error in closeCodeInit: ${e.message}")
            }

            try {
                PrismSpaceCore.get().onBeforeMainApplicationAttach(this, base)
            } catch (e: Exception) {
                Log.e("App", "Error in onBeforeMainApplicationAttach: ${e.message}")
            }

            mContext = base!!

            try {
                AppManager.doAttachBaseContext(base)
            } catch (e: Exception) {
                Log.e("App", "Error in doAttachBaseContext: ${e.message}")
            }

            try {

                PrismSpaceCore.get().onAfterMainApplicationAttach(this, base)

            } catch (e: Exception) {

                Log.e("App", "Error in onAfterMainApplicationAttach: ${e.message}")

            }
        } catch (e: Exception) {
            Log.e("App", "Critical error in attachBaseContext: ${e.message}")
            if (base != null) {
                mContext = base
            }
        }
    }

    override fun onCreate() {
        try {
            super.onCreate()
            PrismSpaceCore.get().preWarmEngine()
            AppManager.doOnCreate(mContext)
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    val submitted = CrashLogSubmitter.submitPendingCrashLogs(this@App)
                    Log.d("App", "Submitted pending engine crash logs: $submitted")
                }.onFailure {
                    Log.e("App", "Error submitting pending crash logs: ${it.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("App", "Error in onCreate: ${e.message}")
        }
    }
}
