package com.prismspace.container.crash

import android.content.Context

object CrashLogSubmitter {
    fun submitPendingCrashLogs(context: Context): Int {
        return CrashLogSubmitterImpl.submitPendingCrashLogs(context)
    }
}

