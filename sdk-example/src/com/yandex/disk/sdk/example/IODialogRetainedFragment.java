/* Copyright (c) 2013 Yandex LLC
 *
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 * License agreement on use of Toolkit
 * «SDK Яндекс.Диска» available at: http://legal.yandex.ru/sdk_agreement
 */

package com.yandex.disk.sdk.example;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;

public class IODialogRetainedFragment extends Fragment {

    protected Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        handler = new Handler();
    }

    protected void sendException(final Exception ex) {
        handler.post(new Runnable() {
            @Override
            public void run () {
                IODialogFragment targetFragment = (IODialogFragment) getTargetFragment();
                if (targetFragment != null) {
                    targetFragment.sendException(ex);
                }
            }
        });
    }
}
