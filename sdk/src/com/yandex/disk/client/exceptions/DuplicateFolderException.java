package com.yandex.disk.client.exceptions;

/**
 * Author: abakumov
 * Created: 8/30/11 4:31 PM
 */
public class DuplicateFolderException extends WebdavException {
    public DuplicateFolderException(String detailMessage) {
        super(detailMessage);
    }
}
