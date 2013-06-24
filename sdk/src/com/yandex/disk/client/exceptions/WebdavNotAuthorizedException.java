package com.yandex.disk.client.exceptions;

public class WebdavNotAuthorizedException extends WebdavException {
    public WebdavNotAuthorizedException(String detailMessage) {
        super(detailMessage);
    }
}
