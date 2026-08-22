package com.prismspace.container.core.system.pm;

import android.os.Parcel;
import android.os.Parcelable;


public class PPackageUserState implements Parcelable {
    public boolean installed;
    public boolean stopped;
    public boolean hidden;

    public PPackageUserState() {
        this.installed = false;
        this.stopped = true;
        this.hidden = false;
    }

    public static PPackageUserState create() {
        PPackageUserState state = new PPackageUserState();
        state.installed = true;
        return state;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.installed ? (byte) 1 : (byte) 0);
        dest.writeByte(this.stopped ? (byte) 1 : (byte) 0);
        dest.writeByte(this.hidden ? (byte) 1 : (byte) 0);
    }

    protected PPackageUserState(Parcel in) {
        this.installed = in.readByte() != 0;
        this.stopped = in.readByte() != 0;
        this.hidden = in.readByte() != 0;
    }

    public PPackageUserState(PPackageUserState state) {
        this.installed = state.installed;
        this.stopped = state.stopped;
        this.hidden = state.hidden;
    }

    public static final Parcelable.Creator<PPackageUserState> CREATOR = new Parcelable.Creator<PPackageUserState>() {
        @Override
        public PPackageUserState createFromParcel(Parcel source) {
            return new PPackageUserState(source);
        }

        @Override
        public PPackageUserState[] newArray(int size) {
            return new PPackageUserState[size];
        }
    };
}

