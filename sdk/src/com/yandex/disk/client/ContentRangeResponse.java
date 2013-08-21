package com.yandex.disk.client;

public class ContentRangeResponse {
    private final long start, size;

    public ContentRangeResponse(long start, long size) {
        this.start = start;
        this.size = size;
    }

    public long getStart() {
        return start;
    }

    public long getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "ContentRangeResponse{"+
                "start="+start+
                ", size="+size+
                '}';
    }
}
