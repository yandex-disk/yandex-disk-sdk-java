package com.yandex.disk.client.exceptions;

/**
 * Author: abakumov
 * Created: 9/2/11 9:24 PM
 */
public class WebdavFileNotFoundException extends WebdavException {
    public WebdavFileNotFoundException(String detailMessage) {
        super(detailMessage);
    }
}
