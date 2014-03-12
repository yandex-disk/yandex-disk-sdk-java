/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client.exceptions;

public class FileDownloadException extends WebdavException {

    public FileDownloadException(String msg) {
        super(msg);
    }
}
