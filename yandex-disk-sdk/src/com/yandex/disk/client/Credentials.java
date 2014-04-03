/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client;

import android.os.Parcel;
import android.os.Parcelable;
import org.apache.http.message.AbstractHttpMessage;

public class Credentials implements Parcelable {

    private String user, token;

    public Credentials(String user, String token) {
        this.user = user;
        this.token = token;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void addAuthHeader(AbstractHttpMessage req) {
        req.addHeader("X-Yandex-SDK-Version", "android, 1.0");
        req.addHeader("Authorization", "OAuth "+token);
    }

    public static final Parcelable.Creator<Credentials> CREATOR = new Parcelable.Creator<Credentials>() {

        public Credentials createFromParcel(Parcel parcel) {
            return new Credentials(parcel.readString(), parcel.readString());
        }

        public Credentials[] newArray(int size) {
            return new Credentials[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(user);
        parcel.writeString(token);
    }
}
