/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client;

import android.util.Log;
import org.apache.http.HttpEntity;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class ListParser extends Parser {

    private static final String TAG = "ListParser";

    public static final String SERVER_ENCODING = "UTF-8";

    private ListParsingHandler handler;
    private int parsedCount;

    private ListItem.Builder builder;
    private boolean isStatusOK;

    public ListParser(HttpEntity entity, ListParsingHandler handler)
            throws XmlPullParserException, IOException {
        super(entity.getContent(), SERVER_ENCODING);
        this.handler = handler;
        this.parsedCount = 0;
    }

    @Override
    public void tagStart(String path) {
        if ("/multistatus/response".equals(path)) {
            builder = new ListItem.Builder();
        }
    }

    @Override
    public void tagEnd(String path, String text)
            throws UnsupportedEncodingException {
        if ("/multistatus/response/href".equals(path)) {
            String href = URLDecoder.decode(text, SERVER_ENCODING);
            if (href.endsWith("/")) {
                href = href.substring(0, href.length()-1);
            }
            builder.addFullPath(href);
        } else if ("/multistatus/response/propstat/status".equals(path)) {
            isStatusOK = "HTTP/1.1 200 OK".equals(text);
        } else if (isStatusOK) {
            if ("/multistatus/response/propstat/prop/displayname".equals(path)) {
                builder.addDisplayName(text);
            } else if ("/multistatus/response/propstat/prop/getcontentlength".equals(path)) {
                builder.addContentLength(text);
            } else if ("/multistatus/response/propstat/prop/getlastmodified".equals(path)) {
                builder.addLastModified(text);
            } else if ("/multistatus/response/propstat/prop/getetag".equals(path)) {
                builder.addEtag(text);
            } else if ("/multistatus/response/propstat/prop/alias_enabled".equals(path)) {
                builder.addAliasEnabled(parseBoolean(text));
            } else if ("/multistatus/response/propstat/prop/visible".equals(path)) {
                builder.addVisible(parseBoolean(text));
            } else if ("/multistatus/response/propstat/prop/resourcetype/collection".equals(path)) {
                builder.addCollection();
            } else if ("/multistatus/response/propstat/prop/getcontenttype".equals(path)) {
                builder.addContentType(text);
            } else if ("/multistatus/response/propstat/prop/shared".equals(path)) {
                builder.addShared(parseBoolean(text));
            } else if ("/multistatus/response/propstat/prop/readonly".equals(path)) {
                builder.addReadOnly(parseBoolean(text));
            } else if ("/multistatus/response/propstat/prop/owner_name".equals(path)) {
                builder.addOwnerName(text);
            }
        } else if ("/multistatus/response".equals(path)) {
            ListItem item = builder.build();
            Log.d(TAG, "item: "+item);
            if (handler.handleItem(item)) {
                parsedCount++;
            }
        }
    }

    @Override
    public void parse()
            throws IOException, XmlPullParserException {
        super.parse();
        handler.onPageFinished(parsedCount);
    }

    public int getParsedCount() {
        return parsedCount;
    }

    private static boolean parseBoolean(String text) {
        try {
            return Integer.parseInt(text) == 1;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
