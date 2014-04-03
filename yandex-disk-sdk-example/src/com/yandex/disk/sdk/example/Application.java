/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.sdk.example;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LogCatHandler.setup(this);
    }
}
