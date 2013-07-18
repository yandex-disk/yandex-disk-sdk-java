/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client.exceptions;

public class DuplicateFolderException extends WebdavException {
    public DuplicateFolderException(String detailMessage) {
        super(detailMessage);
    }
}
