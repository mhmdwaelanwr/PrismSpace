package com.prismspace.container.utils

import java.io.Closeable
import java.io.IOException

object CloseUtils {
    @JvmStatic
    fun close(vararg closeables: Closeable?) {
        for (closeable in closeables) {
            if (closeable != null) {
                try {
                    closeable.close()
                } catch (_: IOException) {
                    // Keep legacy behavior: ignore close failures.
                }
            }
        }
    }
}

