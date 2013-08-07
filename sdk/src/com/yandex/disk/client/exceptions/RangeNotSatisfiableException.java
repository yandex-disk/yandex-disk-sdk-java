package com.yandex.disk.client.exceptions;

public class RangeNotSatisfiableException extends FileDownloadException {

    public RangeNotSatisfiableException(String msg) {
        super(msg);
    }
}
