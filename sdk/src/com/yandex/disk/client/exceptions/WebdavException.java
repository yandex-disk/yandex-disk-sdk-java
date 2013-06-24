package com.yandex.disk.client.exceptions;

/**
 * Author: abakumov
 * Created: 7/6/11 3:10 PM
 */
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
