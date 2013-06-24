package com.yandex.disk.client;

public abstract class ListParsingHandler {

    public void onPageFinished(int itemsOnPage) {
    }

    public boolean hasCancelled() {
        return false;
    }

    public abstract boolean handleItem(ListItem item);
}
