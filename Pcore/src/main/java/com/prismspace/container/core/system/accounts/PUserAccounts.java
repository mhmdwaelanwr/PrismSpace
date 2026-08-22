package com.prismspace.container.core.system.accounts;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PUserAccounts implements Parcelable {
    public final Object lock = new Object();

    public int userId;
    public List<PAccount> accounts = new ArrayList<>();

    public Account[] toAccounts() {
        List<Account> local = new ArrayList<>();
        for (PAccount account : accounts) {
            local.add(account.account);
        }
        return local.toArray(new Account[]{});
    }

    public PAccount addAccount(Account account) {
        PAccount bAccount = new PAccount();
        bAccount.account = account;
        accounts.add(bAccount);
        return bAccount;
    }

    public PAccount getAccount(Account account) {
        for (PAccount bAccount : accounts) {
            if (bAccount.isMatch(account))
                return bAccount;
        }
        return null;
    }

    public boolean delAccount(Account account) {
        PAccount bAccount = getAccount(account);
        return accounts.remove(bAccount);
    }


    public Map<String, Integer> getVisibility(Account account) {
        PAccount bAccount = getAccount(account);
        if (bAccount == null)
            return new HashMap<>();
        return bAccount.visibility;
    }

    public Map<String, String> getAccountUserData(Account account) {
        PAccount bAccount = getAccount(account);
        if (bAccount == null)
            return new HashMap<>();
        return bAccount.accountUserData;
    }

    public Map<String, String> getAuthToken(Account account) {
        PAccount bAccount = getAccount(account);
        if (bAccount == null)
            return new HashMap<>();
        return bAccount.authTokens;
    }

    public Account[] getAccountsByType(String type) {
        List<Account> local = new ArrayList<>();
        for (PAccount account : accounts) {
            if (account.account.type.equals(type)) {
                local.add(account.account);
            }
        }
        return local.toArray(new Account[]{});
    }

    public void updateLastAuthenticatedTime(Account account) {
        PAccount bAccount = getAccount(account);
        if (bAccount != null) {
            bAccount.updateLastAuthenticatedTime = System.currentTimeMillis();
        }
    }

    public long findAccountLastAuthenticatedTime(Account account) {
        PAccount bAccount = getAccount(account);
        if (bAccount != null) {
            return bAccount.updateLastAuthenticatedTime;
        }
        return -1;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.userId);
        dest.writeTypedList(this.accounts);
    }

    public void readFromParcel(Parcel source) {
        this.userId = source.readInt();
        this.accounts = source.createTypedArrayList(PAccount.CREATOR);
    }

    public PUserAccounts() {
    }

    protected PUserAccounts(Parcel in) {
        this.userId = in.readInt();
        this.accounts = in.createTypedArrayList(PAccount.CREATOR);
    }

    public static final Creator<PUserAccounts> CREATOR = new Creator<PUserAccounts>() {
        @Override
        public PUserAccounts createFromParcel(Parcel source) {
            return new PUserAccounts(source);
        }

        @Override
        public PUserAccounts[] newArray(int size) {
            return new PUserAccounts[size];
        }
    };
}

