package com.prismspace.container.fake.provider;

import android.content.Context;
import android.content.pm.ProviderInfo;
import android.net.Uri;

import java.io.File;
import java.util.List;

import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.app.PActivityThread;
import com.prismspace.container.utils.compat.PuildCompat;


public class FileProviderHandler {

    public static Uri convertFileUri(Context context, Uri uri) {
        if (PuildCompat.isN()) {
            File file = convertFile(context, uri);
            if (file == null)
                return null;
            return PrismSpaceCore.getBStorageManager().getUriForFile(file.getAbsolutePath());
        }
        return uri;
    }

    public static File convertFile(Context context, Uri uri) {
        List<ProviderInfo> providers = PActivityThread.getProviders();
        for (ProviderInfo provider : providers) {
            try {
                File fileForUri = FileProvider.getFileForUri(context, provider.authority, uri);
                if (fileForUri != null && fileForUri.exists()) {
                    return fileForUri;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}

