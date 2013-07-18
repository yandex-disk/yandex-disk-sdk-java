/* Copyright (c) 2013 Yandex LLC
 *
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 * License agreement on use of Toolkit
 * «SDK Яндекс.Диска» available at: http://legal.yandex.ru/sdk_agreement
 */

package com.yandex.disk.client.exceptions;

public class WebdavUserNotInitialized extends WebdavException {

    /**
     * TODO check and translate
     * <br/>Пользователь стандартных клиентов обязан хотя бы раз зайти в веб-версию или воспользоваться нашим клиентом
     * для инициализации ему диска. В противном случае клиент получит 403 с телом "bad sid".
     */
    public WebdavUserNotInitialized(String detailMessage) {
        super(detailMessage);
    }
}
