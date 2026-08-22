package com.prismspace.container.fake.service.context.providers;

import android.os.IInterface;


public interface PContentProvider {
    IInterface wrapper(final IInterface contentProviderProxy, final String appPkg);
}

