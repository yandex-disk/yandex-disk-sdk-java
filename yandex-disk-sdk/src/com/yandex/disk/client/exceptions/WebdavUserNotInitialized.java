/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client.exceptions;

public class WebdavUserNotInitialized extends WebdavException {

    /**
     * 403: User have to check http://disk.yandex.ru once before start using this client
     */
    public WebdavUserNotInitialized(String detailMessage) {
        super(detailMessage);
    }
}
