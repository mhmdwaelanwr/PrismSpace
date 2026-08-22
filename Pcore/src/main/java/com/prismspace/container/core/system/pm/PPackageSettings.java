package com.prismspace.container.core.system.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.AtomicFile;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.prismspace.container.core.env.PEnvironment;
import com.prismspace.container.core.system.user.PUserHandle;
import com.prismspace.container.entity.pm.InstallOption;
import com.prismspace.container.utils.CloseUtils;
import com.prismspace.container.utils.FileUtils;


public class PPackageSettings implements Parcelable {
    public PPackage pkg;
    public int appId;
    public InstallOption installOption;
    public Map<Integer, PPackageUserState> userState = new HashMap<>();
    static final PPackageUserState DEFAULT_USER_STATE = new PPackageUserState();

    public PPackageSettings() {
    }

    public List<PPackageUserState> getUserState() {
        return new ArrayList<>(userState.values());
    }

    public List<Integer> getUserIds() {
        return new ArrayList<>(userState.keySet());
    }

    public void setInstalled(boolean inst, int userId) {
        modifyUserState(userId).installed = inst;
    }

    public boolean getInstalled(int userId) {
        return readUserState(userId).installed;
    }

    public boolean getStopped(int userId) {
        return readUserState(userId).stopped;
    }

    public void setStopped(boolean stop, int userId) {
        modifyUserState(userId).stopped = stop;
    }

    public boolean getHidden(int userId) {
        return readUserState(userId).hidden;
    }

    public void setHidden(boolean hidden, int userId) {
        modifyUserState(userId).hidden = hidden;
    }

    public void removeUser(int userId) {
        userState.remove(userId);
    }

    public PPackageUserState readUserState(int userId) {
        PPackageUserState state = userState.get(userId);
        if (state == null) {
            state = new PPackageUserState();
        }
        state = new PPackageUserState(state);
        

        if (userId == PUserHandle.USER_ALL) {
            state.installed = true;
        }
        return state;
    }

    private PPackageUserState modifyUserState(int userId) {
        PPackageUserState state = userState.get(userId);
        if (state == null) {
            state = new PPackageUserState();
            userState.put(userId, state);
        }
        return state;
    }

    public boolean save() {
        synchronized (this) {
            Parcel parcel = Parcel.obtain();
            AtomicFile atomicFile = new AtomicFile(PEnvironment.getPackageConf(pkg.packageName));
            FileOutputStream fileOutputStream = null;
            try {
                writeToParcel(parcel, 0);
                parcel.setDataPosition(0);
                fileOutputStream = atomicFile.startWrite();
                FileUtils.writeParcelToOutput(parcel, fileOutputStream);
                atomicFile.finishWrite(fileOutputStream);
                return true;
            } catch (Throwable e) {
                e.printStackTrace();
                atomicFile.failWrite(fileOutputStream);
                return false;
            } finally {
                parcel.recycle();
                CloseUtils.close(fileOutputStream);
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.pkg, flags);
        dest.writeInt(this.appId);
        dest.writeParcelable(this.installOption, flags);
        dest.writeInt(this.userState.size());
        for (Map.Entry<Integer, PPackageUserState> entry : this.userState.entrySet()) {
            dest.writeValue(entry.getKey());
            dest.writeParcelable(entry.getValue(), flags);
        }
    }

    protected PPackageSettings(Parcel in) {
        this.pkg = in.readParcelable(PPackage.class.getClassLoader());
        this.appId = in.readInt();
        this.installOption = in.readParcelable(InstallOption.class.getClassLoader());
        int userStateSize = in.readInt();
        this.userState = new HashMap<Integer, PPackageUserState>(userStateSize);
        for (int i = 0; i < userStateSize; i++) {
            Integer key = (Integer) in.readValue(Integer.class.getClassLoader());
            PPackageUserState value = in.readParcelable(PPackageUserState.class.getClassLoader());
            this.userState.put(key, value);
        }
    }

    public static final Creator<PPackageSettings> CREATOR = new Creator<PPackageSettings>() {
        @Override
        public PPackageSettings createFromParcel(Parcel source) {
            return new PPackageSettings(source);
        }

        @Override
        public PPackageSettings[] newArray(int size) {
            return new PPackageSettings[size];
        }
    };
}

