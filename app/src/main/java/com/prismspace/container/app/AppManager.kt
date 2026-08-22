package com.prismspace.container.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log


object AppManager {
    private const val TAG = "AppManager"

    @JvmStatic
    val mPrismSpaceLoader by lazy {
        try {
            PrismSpaceLoader()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating PrismSpaceLoader: ${e.message}")

            PrismSpaceLoader() 
        }
    }

    @JvmStatic
    val mPrismSpaceCore by lazy {
        try {
            mPrismSpaceLoader.getPrismSpaceCore()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting PrismSpaceCore: ${e.message}")
            throw e 
        }
    }

    @JvmStatic
    val mRemarkSharedPreferences: SharedPreferences by lazy {
        try {
            App.getContext().getSharedPreferences("UserRemark", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating SharedPreferences: ${e.message}")
            throw e 
        }
    }

    fun doAttachBaseContext(context: Context) {
        try {
            mPrismSpaceLoader.attachBaseContext(context)
            mPrismSpaceLoader.addLifecycleCallback()
        } catch (e: Exception) {
            Log.e(TAG, "Error in doAttachBaseContext: ${e.message}")
            
        }
    }

    fun doOnCreate(context: Context) {
        try {
            mPrismSpaceLoader.doOnCreate(context)
            initThirdService(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error in doOnCreate: ${e.message}")
            
        }
    }

    private fun initThirdService(@Suppress("UNUSED_PARAMETER") context: Context) {
        try {
            
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in initThirdService: ${e.message}")
        }
    }
}

