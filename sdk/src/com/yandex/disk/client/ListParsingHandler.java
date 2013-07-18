/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client;

public abstract class ListParsingHandler {

    public void onPageFinished(int itemsOnPage) {
    }

    public boolean hasCancelled() {
        return false;
    }

    public abstract boolean handleItem(ListItem item);
}
