package com.yandex.disk.client.exceptions;

public class RangeNotSatisfiableException extends WebdavException {

    public RangeNotSatisfiableException(String msg) {
        super(msg);
    }
}
