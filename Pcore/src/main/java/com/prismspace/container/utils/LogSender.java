package com.prismspace.container.utils;

import java.io.File;


public class LogSender {
    private static final String TAG = "LogSender";

    public static String send(String chatId, File logFile, String caption) {
        Slog.i(TAG, "Log upload is disabled by compliance hardening");
        return null;
    }
}
