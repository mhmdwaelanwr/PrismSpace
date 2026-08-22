package com.prismspace.container.core.system.user;

import android.os.Parcel;
import android.os.Parcelable;


public class PUserInfo implements Parcelable {
    public int id;
    public PUserStatus status;
    public String name;
    public long createTime;

    PUserInfo() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeInt(this.status == null ? -1 : this.status.ordinal());
        dest.writeString(this.name);
        dest.writeLong(this.createTime);
    }

    protected PUserInfo(Parcel in) {
        this.id = in.readInt();
        int tmpStatus = in.readInt();
        this.status = tmpStatus == -1 ? null : PUserStatus.values()[tmpStatus];
        this.name = in.readString();
        this.createTime = in.readLong();
    }

    public static final Creator<PUserInfo> CREATOR = new Creator<PUserInfo>() {
        @Override
        public PUserInfo createFromParcel(Parcel source) {
            return new PUserInfo(source);
        }

        @Override
        public PUserInfo[] newArray(int size) {
            return new PUserInfo[size];
        }
    };

    @Override
    public String toString() {
        return "PUserInfo{" +
                "id=" + id +
                ", status=" + status +
                ", name='" + name + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}

