package com.prismspace.container.utils.compat;

import android.os.IBinder;
import android.os.IInterface;

import black.android.app.BRApplicationThreadNative;
import black.android.app.BRIApplicationThreadOreoStub;

public class ApplicationThreadCompat {

    public static IInterface asInterface(IBinder binder) {
        if (PuildCompat.isOreo()) {
            return BRIApplicationThreadOreoStub.get().asInterface(binder);
        }
        return BRApplicationThreadNative.get().asInterface(binder);
    }
}

