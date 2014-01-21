/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.AbstractHttpEntity;

import com.yandex.disk.client.exceptions.CancelledUploadingException;

public class FileProgressHttpEntity extends AbstractHttpEntity {

    private File file;
    private long start;
    private ProgressListener progressListener;

    public FileProgressHttpEntity(File file, long start, ProgressListener progressListener) {
        this.file = file;
        this.start = start;
        this.progressListener = progressListener;
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public long getContentLength() {
        return file.length()-start;
    }

    @Override
    public InputStream getContent()
            throws IOException, IllegalStateException {
        return new FileInputStream(file);
    }

    @Override
    public void writeTo(OutputStream outputStream)
            throws IOException {
        if (outputStream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
        InputStream inputStream = new FileInputStream(file);
        if (start > 0) {
            long skipped = inputStream.skip(start);
        }
        long loaded = 0;
        updateProgress(loaded);
        try {
            byte[] buf = new byte[1024];
            int count;
            while ((count = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, count);
                loaded += count;
                updateProgress(loaded);
            }
            outputStream.flush();
        } finally {
            inputStream.close();
        }
    }

    @Override
    public boolean isStreaming() {
        return false;
    }

    private void updateProgress(long loaded)
            throws CancelledUploadingException {
        if (progressListener != null) {
            if (progressListener.hasCancelled()) {
                throw new CancelledUploadingException();
            }
            progressListener.updateProgress(loaded+start, getContentLength()+start);
        }
    }

    public File getFile() {
        return file;
    }
}
