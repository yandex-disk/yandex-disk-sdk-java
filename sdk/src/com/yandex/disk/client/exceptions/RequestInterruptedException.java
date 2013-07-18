/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client.exceptions;

public class RequestInterruptedException extends WebdavException {

    public RequestInterruptedException(String detailMessage) {
        super(detailMessage);
    }

    public RequestInterruptedException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public RequestInterruptedException(Throwable throwable) {
        super(throwable);
    }
}
