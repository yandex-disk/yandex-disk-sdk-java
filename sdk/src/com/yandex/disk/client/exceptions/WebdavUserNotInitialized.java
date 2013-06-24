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
