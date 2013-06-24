package com.yandex.disk.client.exceptions;

public class ServerWebdavException extends WebdavException {

    public ServerWebdavException(String detailMessage) {
        super(detailMessage);
    }

    public ServerWebdavException() {
        super();
    }

}
