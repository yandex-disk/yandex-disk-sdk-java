/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client;

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
        } else if ("/multistatus/response/propstat".equals(path)) {
            isStatusOK = false;
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
            builder.setFullPath(href);
        } else if ("/multistatus/response/propstat/status".equals(path)) {
            isStatusOK = "HTTP/1.1 200 OK".equals(text);
        } else if ("/multistatus/response".equals(path)) {
            ListItem item = builder.build();
            if (handler.handleItem(item)) {
                parsedCount++;
            }
        } else if (isStatusOK) {
            if ("/multistatus/response/propstat/prop/displayname".equals(path)) {
                builder.setDisplayName(text);
            } else if ("/multistatus/response/propstat/prop/getcontentlength".equals(path)) {
                builder.setContentLength(parseLong(text));
            } else if ("/multistatus/response/propstat/prop/getlastmodified".equals(path)) {
                builder.setLastModified(text);
            } else if ("/multistatus/response/propstat/prop/getetag".equals(path)) {
                builder.setEtag(text);
            } else if ("/multistatus/response/propstat/prop/alias_enabled".equals(path)) {
                builder.setAliasEnabled(parseBooleanAsNumber(text));
            } else if ("/multistatus/response/propstat/prop/visible".equals(path)) {
                builder.setVisible(parseBooleanAsNumber(text));
            } else if ("/multistatus/response/propstat/prop/resourcetype/collection".equals(path)) {
                builder.addCollection();
            } else if ("/multistatus/response/propstat/prop/getcontenttype".equals(path)) {
                builder.setContentType(text);
            } else if ("/multistatus/response/propstat/prop/shared".equals(path)) {
                builder.setShared(Boolean.parseBoolean(text));
            } else if ("/multistatus/response/propstat/prop/readonly".equals(path)) {
                builder.setReadOnly(Boolean.parseBoolean(text));
            } else if ("/multistatus/response/propstat/prop/owner_name".equals(path)) {
                builder.setOwnerName(text);
            } else if ("/multistatus/response/propstat/prop/public_url".equals(path)) {
                builder.setPublicUrl(text);
            } else if ("/multistatus/response/propstat/prop/etime".equals(path)) {
                builder.setEtime(parseLong(text));
            } else if ("/multistatus/response/propstat/prop/mediatype".equals(path)) {
                builder.setMediaType(text);
            } else if ("/multistatus/response/propstat/prop/mpfs_file_id".equals(path)) {
                builder.setMpfsFileId(text);
            } else if ("/multistatus/response/propstat/prop/hasthumbnail".equals(path)) {
                builder.setHasThumbnail(parseBooleanAsNumber(text));
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

    private static boolean parseBooleanAsNumber(String text) {
        try {
            return Integer.parseInt(text) == 1;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static long parseLong(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException nfe) {
            return  0;
        }
    }
}
