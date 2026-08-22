package com.prismspace.container.fake.frameworks;

import android.os.IBinder;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

import com.prismspace.container.app.PActivityThread;
import com.prismspace.container.core.system.ServiceManager;
import com.prismspace.container.core.system.location.IBLocationManagerService;
import com.prismspace.container.entity.location.PCell;
import com.prismspace.container.entity.location.PLocation;


public class PLocationManager extends PlackManager<IBLocationManagerService> {
    private static final PLocationManager sLocationManager = new PLocationManager();

    public static final int CLOSE_MODE = 0;
    public static final int GLOBAL_MODE = 1;
    public static final int OWN_MODE = 2;

    public static PLocationManager get() {
        return sLocationManager;
    }

    @Override
    protected String getServiceName() {
        return ServiceManager.LOCATION_MANAGER;
    }

    public static boolean isFakeLocationEnable() {
        return get().getPattern(PActivityThread.getUserId(), PActivityThread.getAppPackageName()) != CLOSE_MODE;
    }

    public static void disableFakeLocation(int userId,String pkg){
        get().setPattern(userId,pkg,CLOSE_MODE);
    }

    public void setPattern(int userId, String pkg, int pattern) {
        try {
            getService().setPattern(userId, pkg, pattern);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public int getPattern(int userId, String pkg) {
        try {
            return getService().getPattern(userId, pkg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return CLOSE_MODE;
    }

    public void setCell(int userId, String pkg, PCell cell) {
        try {
            getService().setCell(userId, pkg, cell);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setAllCell(int userId, String pkg, List<PCell> cells) {
        try {
            getService().setAllCell(userId, pkg, cells);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public List<PCell> getNeighboringCell(int userId, String pkg) {
        try {
            return getService().getNeighboringCell(userId, pkg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<PCell> getGlobalNeighboringCell() {
        try {
            return getService().getGlobalNeighboringCell();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setNeighboringCell(int userId, String pkg, List<PCell> cells) {
        try {
            getService().setNeighboringCell(userId, pkg, cells);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setGlobalCell(PCell cell) {
        try {
            getService().setGlobalCell(cell);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setGlobalAllCell(List<PCell> cells) {
        try {
            getService().setGlobalAllCell(cells);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setGlobalNeighboringCell(List<PCell> cells) {
        try {
            getService().setGlobalNeighboringCell(cells);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public PCell getCell(int userId, String pkg) {
        try {
            return getService().getCell(userId, pkg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<PCell> getAllCell(int userId, String pkg) {
        try {
            return getService().getAllCell(userId, pkg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public void setLocation(int userId, String pkg, PLocation location) {
        try {
            getService().setLocation(userId, pkg, location);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public PLocation getLocation(int userId, String pkg) {
        try {
            return getService().getLocation(userId, pkg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setGlobalLocation(PLocation location) {
        try {
            getService().setGlobalLocation(location);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public PLocation getGlobalLocation() {
        try {
            return getService().getGlobalLocation();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void requestLocationUpdates(IBinder listener) {
        try {
            getService().requestLocationUpdates(listener, PActivityThread.getAppPackageName(), PActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void removeUpdates(IBinder listener) {
        try {
            getService().removeUpdates(listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}

