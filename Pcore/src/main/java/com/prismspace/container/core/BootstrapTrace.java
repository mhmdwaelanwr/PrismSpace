package com.prismspace.container.core;

import android.content.Context;

import com.prismspace.container.PrismSpaceCore;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class BootstrapTrace {
    private static final Object sTraceLock = new Object();
    private static final String TRACE_FILE_NAME = "prism_bootstrap_trace.log";
    private static final ExecutorService sTraceExecutor =
            Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "PrismBootstrapTrace");
                }
            });

    private BootstrapTrace() {
    }

    public static String getTraceFilePath() {
        try {
            final Context context = PrismSpaceCore.getContext();
            if (context == null) {
                return null;
            }
            final File file = new File(context.getFilesDir(), TRACE_FILE_NAME);
            return file.getAbsolutePath();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void trace(String event) {
        if (event == null) {
            return;
        }
        final long now = System.currentTimeMillis();
        sTraceExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final String path = getTraceFilePath();
                if (path == null) {
                    return;
                }
                synchronized (sTraceLock) {
                    try (FileWriter writer = new FileWriter(path, true)) {
                        writer.write(now + " " + event + "\n");
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
    }
}

