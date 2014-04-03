/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public abstract class Parser {

    private XmlPullParser xml;

    private static XmlPullParser init()
            throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newPullParser();
    }

    public Parser(Reader reader)
            throws XmlPullParserException {
        xml = init();
        xml.setInput(reader);
    }

    public Parser(InputStream in, String encoding)
            throws XmlPullParserException {
        xml = init();
        xml.setInput(in, encoding);
    }

    public abstract void tagStart(String path)
            throws IOException;

    public abstract void tagEnd(String path, String text)
            throws IOException;

    public void parse()
            throws IOException, XmlPullParserException {
        StringBuilder path = new StringBuilder();
        String text = null;

        int event;
        do {
            event = xml.next();

            switch (event) {
                case XmlPullParser.START_TAG:
                    path.append("/").append(xml.getName());
                    tagStart(path.toString());
                    break;

                case XmlPullParser.TEXT:
                    text = xml.getText();
                    break;

                case XmlPullParser.END_TAG:
                    tagEnd(path.toString(), text);
                    path.setLength(path.length()-xml.getName().length()-1);
                    break;
                default:
                    break;
            }

        } while (event != XmlPullParser.END_DOCUMENT);
    }
}