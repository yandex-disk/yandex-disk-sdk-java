/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client.exceptions;

public class DownloadNoSpaceAvailableException extends WebdavException {

    private final String destinationFolder;
    private final long length;

    public DownloadNoSpaceAvailableException(String destinationFolder, long length) {
        super();
        this.destinationFolder = destinationFolder;
        this.length = length;
    }

    public String getDestinationFolder() {
        return destinationFolder;
    }

    public long getLength() {
        return length;
    }
}
