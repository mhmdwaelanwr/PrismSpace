// IFakeLocationManager.aidl
package com.prismspace.container.core.system.location;

import com.prismspace.container.entity.location.PLocation;
import com.prismspace.container.entity.location.PCell;
import android.os.IBinder;

import java.util.List;


interface IBLocationManagerService {
    int getPattern(int userId, String pkg);

    void setPattern(int userId, String pkg, int mode);

    void setCell(int userId, String pkg,in  PCell cell);

    void setAllCell(int userId, String pkg,in  List<PCell> cell);

    void setNeighboringCell(int userId, String pkg,in  List<PCell> cells);
    List<PCell> getNeighboringCell(int userId, String pkg);

    void setGlobalCell(in PCell cell);

    void setGlobalAllCell(in List<PCell> cell);

    void setGlobalNeighboringCell(in List<PCell> cell);

    List<PCell> getGlobalNeighboringCell();

    PCell getCell(int userId, String pkg);

    List<PCell> getAllCell(int userId, String pkg);

    void setLocation(int userId, String pkg,in  PLocation location);

    PLocation getLocation(int userId, String pkg);

    void setGlobalLocation(in PLocation location);

    PLocation getGlobalLocation();

    void requestLocationUpdates(in IBinder listener, String packageName, int userId);

    void removeUpdates(in IBinder listener);
}
