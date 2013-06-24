package com.yandex.disk.client;

public interface ProgressListener {
    void updateProgress(long loaded, long total);

    boolean hasCancelled();
}
