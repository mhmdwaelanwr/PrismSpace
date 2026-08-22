package com.prismspace.container.core.system.location;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.AtomicFile;
import android.util.SparseArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import black.android.location.BRILocationListener;
import black.android.location.BRILocationListenerStub;
import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.core.env.PEnvironment;
import com.prismspace.container.core.system.ISystemService;
import com.prismspace.container.entity.location.PCell;
import com.prismspace.container.entity.location.PLocation;
import com.prismspace.container.entity.location.PLocationConfig;
import com.prismspace.container.fake.frameworks.PLocationManager;
import com.prismspace.container.utils.CloseUtils;
import com.prismspace.container.utils.FileUtils;
import com.prismspace.container.utils.Slog;


public class PLocationManagerService extends IBLocationManagerService.Stub implements ISystemService {
    public static final String TAG = "PLocationManagerService";

    private static final PLocationManagerService sService = new PLocationManagerService();
    private final SparseArray<HashMap<String, PLocationConfig>> mLocationConfigs = new SparseArray<>();
    private final PLocationConfig mGlobalConfig = new PLocationConfig();
    private final Map<IBinder, LocationRecord> mLocationListeners = new HashMap<>();
    private final Executor mThreadPool = Executors.newCachedThreadPool();

    public static PLocationManagerService get() {
        return sService;
    }

    private PLocationConfig getOrCreateConfig(int userId, String pkg) {
        synchronized (mLocationConfigs) {
            HashMap<String, PLocationConfig> pkgs = mLocationConfigs.get(userId);
            if (pkgs == null) {
                pkgs = new HashMap<>();
                mLocationConfigs.put(userId, pkgs);
            }
            PLocationConfig config = pkgs.get(pkg);
            if (config == null) {
                config = new PLocationConfig();
                config.pattern = PLocationManager.CLOSE_MODE;
                pkgs.put(pkg, config);
            }
            return config;
        }
    }

    public int getPattern(int userId, String pkg) {
        synchronized (mLocationConfigs) {
            PLocationConfig config = getOrCreateConfig(userId, pkg);
            return config.pattern;
        }
    }

    @Override
    public void setPattern(int userId, String pkg, int pattern) {
        synchronized (mLocationConfigs) {
            getOrCreateConfig(userId, pkg).pattern = pattern;
            save();
        }
    }

    @Override
    public void setCell(int userId, String pkg, PCell cell) {
        synchronized (mLocationConfigs) {
            getOrCreateConfig(userId, pkg).cell = cell;
            save();
        }
    }

    @Override
    public void setAllCell(int userId, String pkg, List<PCell> cells) {
        synchronized (mLocationConfigs) {
            getOrCreateConfig(userId, pkg).allCell = cells;
            save();
        }
    }

    @Override
    public void setNeighboringCell(int userId, String pkg, List<PCell> cells) {
        synchronized (mLocationConfigs) {
            getOrCreateConfig(userId, pkg).allCell = cells;
            save();
        }
    }

    @Override
    public List<PCell> getNeighboringCell(int userId, String pkg) {
        synchronized (mLocationConfigs) {
            return getOrCreateConfig(userId, pkg).allCell;
        }
    }

    @Override
    public void setGlobalCell(PCell cell) {
        synchronized (mGlobalConfig) {
            mGlobalConfig.cell = cell;
            save();
        }
    }

    @Override
    public void setGlobalAllCell(List<PCell> cells) {
        synchronized (mGlobalConfig) {
            mGlobalConfig.allCell = cells;
            save();
        }
    }

    @Override
    public void setGlobalNeighboringCell(List<PCell> cells) {
        synchronized (mGlobalConfig) {
            mGlobalConfig.neighboringCellInfo = cells;
            save();
        }
    }

    @Override
    public List<PCell> getGlobalNeighboringCell() {
        synchronized (mGlobalConfig) {
            return mGlobalConfig.neighboringCellInfo;
        }
    }

    @Override
    public PCell getCell(int userId, String pkg) {
        PLocationConfig config = getOrCreateConfig(userId, pkg);
        switch (config.pattern) {
            case PLocationManager.OWN_MODE:
                return config.cell;
            case PLocationManager.GLOBAL_MODE:
                return mGlobalConfig.cell;
            case PLocationManager.CLOSE_MODE:
            default:
                return null;
        }
    }

    @Override
    public List<PCell> getAllCell(int userId, String pkg) {
        PLocationConfig config = getOrCreateConfig(userId, pkg);
        switch (config.pattern) {
            case PLocationManager.OWN_MODE:
                return config.allCell;
            case PLocationManager.GLOBAL_MODE:
                return mGlobalConfig.allCell;
            case PLocationManager.CLOSE_MODE:
            default:
                return null;
        }
    }

    @Override
    public void setLocation(int userId, String pkg, PLocation location) {
        synchronized (mLocationConfigs) {
            getOrCreateConfig(userId, pkg).location = location;
            save();
        }
    }

    @Override
    public PLocation getLocation(int userId, String pkg) {
        PLocationConfig config = getOrCreateConfig(userId, pkg);
        switch (config.pattern) {
            case PLocationManager.OWN_MODE:
                return config.location;
            case PLocationManager.GLOBAL_MODE:
                return mGlobalConfig.location;
            case PLocationManager.CLOSE_MODE:
            default:
                return null;
        }
    }

    @Override
    public void setGlobalLocation(PLocation location) {
        synchronized (mGlobalConfig) {
            mGlobalConfig.location = location;
            save();
        }
    }

    @Override
    public PLocation getGlobalLocation() {
        synchronized (mGlobalConfig) {
            return mGlobalConfig.location;
        }
    }

    @Override
    public void requestLocationUpdates(IBinder listener, String packageName, int userId) throws RemoteException {
        if (listener == null || !listener.pingBinder()) {
            return;
        }
        if (mLocationListeners.containsKey(listener))
            return;
        listener.linkToDeath(new DeathRecipient() {
            @Override
            public void binderDied() {
                listener.unlinkToDeath(this, 0);
                mLocationListeners.remove(listener);
            }
        }, 0);
        LocationRecord record = new LocationRecord(packageName, userId);
        mLocationListeners.put(listener, record);
        addTask(listener);
    }

    @Override
    public void removeUpdates(IBinder listener) throws RemoteException {
        if (listener == null || !listener.pingBinder()) {
            return;
        }
        mLocationListeners.remove(listener);
    }

    private void addTask(IBinder locationListener) {
        mThreadPool.execute(() -> {
            PLocation lastLocation = null;
            long l = System.currentTimeMillis();
            while (locationListener.pingBinder()) {
                IInterface iInterface = BRILocationListenerStub.get().asInterface(locationListener);
                LocationRecord locationRecord = mLocationListeners.get(locationListener);
                if (locationRecord == null)
                    continue;
                PLocation location = getLocation(locationRecord.userId, locationRecord.packageName);
                if (location == null)
                    continue;
                if (location.equals(lastLocation) && (System.currentTimeMillis() - l) < 3000) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                    continue;
                }
                lastLocation = location;
                l = System.currentTimeMillis();
                PrismSpaceCore.get().getHandler().post(() -> BRILocationListener.get(iInterface).onLocationChanged(location.convert2SystemLocation()));
            }
        });
    }

    public void save() {
        synchronized (mGlobalConfig) {
            synchronized (mLocationConfigs) {
                Parcel parcel = Parcel.obtain();
                AtomicFile atomicFile = new AtomicFile(PEnvironment.getFakeLocationConf());
                FileOutputStream fileOutputStream = null;
                try {
                    mGlobalConfig.writeToParcel(parcel, 0);

                    parcel.writeInt(mLocationConfigs.size());
                    for (int i = 0; i < mLocationConfigs.size(); i++) {
                        int tmpUserId = mLocationConfigs.keyAt(i);
                        HashMap<String, PLocationConfig> configArrayMap = mLocationConfigs.valueAt(i);
                        parcel.writeInt(tmpUserId);
                        parcel.writeMap(configArrayMap);
                    }
                    parcel.setDataPosition(0);
                    fileOutputStream = atomicFile.startWrite();
                    FileUtils.writeParcelToOutput(parcel, fileOutputStream);
                    atomicFile.finishWrite(fileOutputStream);
                } catch (Throwable e) {
                    e.printStackTrace();
                    atomicFile.failWrite(fileOutputStream);
                } finally {
                    parcel.recycle();
                    CloseUtils.close(fileOutputStream);
                }
            }
        }
    }

    public void loadConfig() {
        Parcel parcel = Parcel.obtain();
        InputStream is = null;
        try {
            File fakeLocationConf = PEnvironment.getFakeLocationConf();
            if (!fakeLocationConf.exists()) {
                return;
            }
            is = new FileInputStream(PEnvironment.getFakeLocationConf());
            byte[] bytes = FileUtils.toByteArray(is);
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);

            synchronized (mGlobalConfig) {
                mGlobalConfig.refresh(parcel);
            }

            synchronized (mLocationConfigs) {
                mLocationConfigs.clear();
                int size = parcel.readInt();
                for (int i = 0; i < size; i++) {
                    int userId = parcel.readInt();
                    HashMap<String, PLocationConfig> configArrayMap = parcel.readHashMap(PLocationConfig.class.getClassLoader());
                    mLocationConfigs.put(userId, configArrayMap);
                    Slog.d(TAG, "load userId: " + userId + ", config: " + configArrayMap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Slog.d(TAG, "bad config");
            FileUtils.deleteDir(PEnvironment.getFakeLocationConf());
        } finally {
            parcel.recycle();
            CloseUtils.close(is);
        }
    }

    @Override
    public void systemReady() {
        loadConfig();
        for (IBinder iBinder : mLocationListeners.keySet()) {
            addTask(iBinder);
        }
    }
}

