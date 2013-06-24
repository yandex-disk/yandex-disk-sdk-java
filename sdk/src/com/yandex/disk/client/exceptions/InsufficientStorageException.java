package com.yandex.disk.client.exceptions;

import com.yandex.disk.client.exceptions.ServerWebdavException;

public class InsufficientStorageException extends ServerWebdavException {

    public InsufficientStorageException() {
        super("The server is unable to store the representation needed to complete the request");
    }

}
