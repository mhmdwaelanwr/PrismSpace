package com.prismspace.container.app.dispatcher

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Handler
import android.os.IBinder
import com.prismspace.container.PrismSpaceCore
import com.prismspace.container.entity.ServiceRecord
import com.prismspace.container.entity.UnbindRecord
import com.prismspace.container.proxy.record.ProxyServiceRecord

class AppServiceDispatcher private constructor() {
    private val mService: MutableMap<Intent.FilterComparison, ServiceRecord> = HashMap()
    private val mHandler: Handler = PrismSpaceCore.get().handler

    fun onBind(proxyIntent: Intent?): IBinder? {
        if (proxyIntent == null) {
            return null
        }
        val serviceRecord = ProxyServiceRecord.create(proxyIntent)
        val intent = serviceRecord.mServiceIntent
        val serviceInfo = serviceRecord.mServiceInfo
        if (intent == null || serviceInfo == null) {
            return null
        }

        val service = getOrCreateService(serviceRecord) ?: return null
        intent.setExtrasClassLoader(service.classLoader)

        val record = findRecord(intent) ?: return null
        record.incrementAndGetBindCount(intent)
        if (record.hasBinder(intent)) {
            if (record.isRebind()) {
                service.onRebind(intent)
                record.setRebind(false)
            }
            return record.getBinder(intent)
        }

        return try {
            val iBinder = service.onBind(intent)
            if (iBinder == null) {
                return null
            }
            record.addBinder(intent, iBinder)
            iBinder
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    fun onStartCommand(proxyIntent: Intent?, flags: Int, startId: Int): Int {
        if (proxyIntent == null) {
            return Service.START_NOT_STICKY
        }
        val stubRecord = ProxyServiceRecord.create(proxyIntent)
        if (stubRecord.mServiceIntent == null || stubRecord.mServiceInfo == null) {
            return Service.START_NOT_STICKY
        }

        val service = getOrCreateService(stubRecord) ?: return Service.START_NOT_STICKY
        stubRecord.mServiceIntent.setExtrasClassLoader(service.classLoader)
        val record = findRecord(stubRecord.mServiceIntent) ?: return Service.START_NOT_STICKY
        record.setStartId(stubRecord.mStartId)
        return try {
            val result = service.onStartCommand(stubRecord.mServiceIntent, flags, stubRecord.mStartId)
            PrismSpaceCore.getBActivityManager().onStartCommand(proxyIntent, stubRecord.mUserId)
            result
        } catch (e: Throwable) {
            e.printStackTrace()
            Service.START_NOT_STICKY
        }
    }

    fun onDestroy() {
        if (mService.isNotEmpty()) {
            for (record in mService.values) {
                try {
                    record.getService()?.onDestroy()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
        mService.clear()
    }

    fun onConfigurationChanged(newConfig: Configuration?) {
        if (newConfig == null) {
            return
        }
        if (mService.isNotEmpty()) {
            for (record in mService.values) {
                try {
                    record.getService()?.onConfigurationChanged(newConfig)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun onLowMemory() {
        if (mService.isNotEmpty()) {
            for (record in mService.values) {
                try {
                    record.getService()?.onLowMemory()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun onTrimMemory(level: Int) {
        if (mService.isNotEmpty()) {
            for (record in mService.values) {
                try {
                    record.getService()?.onTrimMemory(level)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun onUnbind(proxyIntent: Intent?): Boolean {
        if (proxyIntent == null) {
            return false
        }
        val stubRecord = ProxyServiceRecord.create(proxyIntent)
        if (stubRecord.mServiceIntent == null || stubRecord.mServiceInfo == null) {
            return false
        }
        val intent = stubRecord.mServiceIntent

        try {
            val unbindRecord: UnbindRecord = PrismSpaceCore.getBActivityManager()
                .onServiceUnbind(proxyIntent, PrismSpaceCore.getUserId()) ?: return false

            val service = getOrCreateService(stubRecord) ?: return false
            stubRecord.mServiceIntent.setExtrasClassLoader(service.classLoader)

            val record = findRecord(intent) ?: return false
            val destroy = unbindRecord.getStartId() == 0
            if (destroy || record.decreaseConnectionCount(intent)) {
                service.onUnbind(intent)
                if (destroy) {
                    service.onDestroy()
                    PrismSpaceCore.getBActivityManager().onServiceDestroy(proxyIntent, PrismSpaceCore.getUserId())
                    mService.remove(Intent.FilterComparison(intent))
                }
                record.setRebind(true)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return false
    }

    fun peekService(intent: Intent?): IBinder? {
        val record = findRecord(intent) ?: return null
        return record.getBinder(intent ?: return null)
    }

    fun stopService(intent: Intent?) {
        if (intent == null) {
            return
        }
        val record = findRecord(intent) ?: return
        val service = record.getService()
        if (service != null) {
            val destroy = record.getStartId() > 0
            try {
                if (destroy) {
                    mHandler.post { record.getService()?.onDestroy() }
                    PrismSpaceCore.getBActivityManager().onServiceDestroy(intent, PrismSpaceCore.getUserId())
                    mService.remove(Intent.FilterComparison(intent))
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private fun findRecord(intent: Intent?): ServiceRecord? {
        if (intent == null) {
            return null
        }
        return mService[Intent.FilterComparison(intent)]
    }

    private fun getOrCreateService(proxyServiceRecord: ProxyServiceRecord): Service? {
        val intent = proxyServiceRecord.mServiceIntent ?: return null
        val serviceInfo: ServiceInfo = proxyServiceRecord.mServiceInfo ?: return null
        val token = proxyServiceRecord.mToken

        val existing = findRecord(intent)
        if (existing?.getService() != null) {
            return existing.getService()
        }

        val service = PrismSpaceCore.currentActivityThread().createService(serviceInfo, token) ?: return null
        val record = ServiceRecord()
        record.setService(service)
        mService[Intent.FilterComparison(intent)] = record
        return service
    }

    companion object {
        @JvmField
        val TAG = "AppServiceDispatcher"

        @JvmField
        val sServiceDispatcher: AppServiceDispatcher = AppServiceDispatcher()

        @JvmStatic
        fun get(): AppServiceDispatcher = sServiceDispatcher
    }
}

