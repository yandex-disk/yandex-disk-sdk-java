/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client.exceptions;

public class SharingException extends WebdavException {
    public SharingException(String detailMessage) {
        super(detailMessage);
    }

    public SharingException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
