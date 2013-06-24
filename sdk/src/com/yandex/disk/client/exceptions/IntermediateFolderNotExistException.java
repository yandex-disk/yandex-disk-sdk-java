package com.yandex.disk.client.exceptions;

public class IntermediateFolderNotExistException extends WebdavException {
    public IntermediateFolderNotExistException(String detailMessage) {
        super(detailMessage);
    }
}
