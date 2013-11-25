/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client;

import java.io.IOException;
import java.io.OutputStream;

public abstract class DownloadListener implements ProgressListener {

    /**
     * Local file length for resuming download. 0 if local file not exist
     */
    public long getLocalLength() {
        return 0;
    }

    /**
     * Length on the server. 0 if not known
     */
    public long getServerLength() {
        return 0;
    }

    /**
     * Used for <tt>If-None-Match</tt> or <tt>If-Range</tt>. MD5 or <tt>null</tt> if not applicable or not known
     * @see <a href="http://tools.ietf.org/html/rfc2616#page-132">rfc 2616</a>
     */
    public String getETag() {
        return null;
    }

    /**
     * Start position for partial content (http code 206)
     */
    public void setStartPosition(long position) {
    }

    /**
     * Content length for partial content (http code 206)
     */
    public void setContentLenght(long lenght) {
    }

    public abstract OutputStream getOutputStream(boolean append)
            throws IOException;

    @Override
    public void updateProgress(long loaded, long total) {
    }

    @Override
    public boolean hasCancelled() {
        return false;
    }
}
