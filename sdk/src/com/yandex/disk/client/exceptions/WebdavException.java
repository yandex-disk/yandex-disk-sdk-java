/* Copyright (c) 2013 Yandex LLC
 *
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 * License agreement on use of Toolkit
 * «SDK Яндекс.Диска» available at: http://legal.yandex.ru/sdk_agreement
 */

package com.yandex.disk.client.exceptions;

public class WebdavException extends Exception {

    public WebdavException() {
        super();
    }

    public WebdavException(String detailMessage) {
        super(detailMessage);
    }

    public WebdavException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public WebdavException(Throwable throwable) {
        super(throwable);
    }
}
