/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.protocol.HttpContext;

public abstract class ListParsingHandler {

    public HttpContext onCreateRequest(HttpPost post, AbstractHttpEntity entity) {
        post.setEntity(entity);
        return null;
    }

    public void onPageFinished(int itemsOnPage) {
    }

    public boolean hasCancelled() {
        return false;
    }

    public abstract boolean handleItem(ListItem item);
}
