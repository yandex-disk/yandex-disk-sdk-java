/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client;

import org.apache.http.message.AbstractHttpMessage;

public class Credentials {

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
}
