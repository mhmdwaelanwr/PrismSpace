package com.prismspace.container.entity

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import java.util.concurrent.atomic.AtomicInteger

class ServiceRecord {
    private var mService: Service? = null
    private val mBounds: MutableMap<Intent.FilterComparison, BoundInfo> = HashMap()
    private var rebind = false
    private var mStartId = 0

    inner class BoundInfo {
        private var mIBinder: IBinder? = null
        private val mBindCount = AtomicInteger(0)

        fun incrementAndGetBindCount(): Int = mBindCount.incrementAndGet()

        fun decrementAndGetBindCount(): Int = mBindCount.decrementAndGet()

        fun getIBinder(): IBinder? = mIBinder

        fun setIBinder(iBinder: IBinder?) {
            mIBinder = iBinder
        }
    }

    fun getStartId(): Int = mStartId

    fun setStartId(startId: Int) {
        mStartId = startId
    }

    fun getService(): Service? = mService

    fun setService(service: Service?) {
        mService = service
    }

    fun getBinder(intent: Intent): IBinder? {
        val boundInfo = getOrCreateBoundInfo(intent)
        return boundInfo.getIBinder()
    }

    fun hasBinder(intent: Intent): Boolean {
        val boundInfo = getOrCreateBoundInfo(intent)
        return boundInfo.getIBinder() != null
    }

    fun addBinder(intent: Intent, iBinder: IBinder) {
        val filterComparison = Intent.FilterComparison(intent)
        val boundInfo = getOrCreateBoundInfo(intent)
        boundInfo.setIBinder(iBinder)
        try {
            val deathRecipient = object : IBinder.DeathRecipient {
                override fun binderDied() {
                    iBinder.unlinkToDeath(this, 0)
                    mBounds.remove(filterComparison)
                }
            }
            iBinder.linkToDeath(deathRecipient, 0)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun incrementAndGetBindCount(intent: Intent): Int {
        val boundInfo = getOrCreateBoundInfo(intent)
        return boundInfo.incrementAndGetBindCount()
    }

    fun decreaseConnectionCount(intent: Intent): Boolean {
        val filterComparison = Intent.FilterComparison(intent)
        val boundInfo = mBounds[filterComparison] ?: return true
        val bindCount = boundInfo.decrementAndGetBindCount()
        return bindCount <= 0
    }

    fun getOrCreateBoundInfo(intent: Intent): BoundInfo {
        val filterComparison = Intent.FilterComparison(intent)
        return mBounds.getOrPut(filterComparison) { BoundInfo() }
    }

    fun isRebind(): Boolean = rebind

    fun setRebind(rebind: Boolean) {
        this.rebind = rebind
    }
}

