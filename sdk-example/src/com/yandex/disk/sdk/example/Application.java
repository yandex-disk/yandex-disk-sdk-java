
package com.yandex.disk.sdk.example;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LogCatHandler.setup(this);
    }
}
