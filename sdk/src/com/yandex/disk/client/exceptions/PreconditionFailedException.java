package com.yandex.disk.client.exceptions;

public class PreconditionFailedException extends WebdavException {
    public PreconditionFailedException(String message) {
        super(message);
    }
}
