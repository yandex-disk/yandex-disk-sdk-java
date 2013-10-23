/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client;

import java.io.IOException;
import java.io.OutputStream;

public interface DownloadListener extends ProgressListener {

    /** 0 if local file not exist */
    long getLocalLength();

    /** 0 if not known */
    long getServerLength();

    OutputStream getOutputStream(boolean append)
            throws IOException;
}
