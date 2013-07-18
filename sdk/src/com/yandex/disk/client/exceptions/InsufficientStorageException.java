/* Copyright (c) 2013 Yandex LLC
 *
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 * License agreement on use of Toolkit
 * «SDK Яндекс.Диска» available at: http://legal.yandex.ru/sdk_agreement
 */

package com.yandex.disk.client.exceptions;

import com.yandex.disk.client.exceptions.ServerWebdavException;

public class InsufficientStorageException extends ServerWebdavException {

    public InsufficientStorageException() {
        super("The server is unable to store the representation needed to complete the request");
    }

}
