/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client;

public class Log {

    public static void d(String tag, String s) {
        System.err.print(tag+": "+s);
    }

    public static void d(String tag, String s, Throwable e) {
        System.err.print(tag+": "+s);
        e.printStackTrace();
    }

    public static void i(String tag, String s) {
        System.err.print(tag+": "+s);
    }

    public static void i(String tag, String s, Throwable e) {
        System.err.print(tag+": "+s);
        e.printStackTrace();
    }

    public static void e(String tag, String s) {
        System.err.print(tag+": "+s);
    }

    public static void e(String tag, String s, Throwable e) {
        System.err.print(tag+": "+s);
        e.printStackTrace();
    }

    public static void w(String tag, String s) {
        System.err.print(tag+": "+s);
    }

    public static void w(String tag, String s, Throwable e) {
        System.err.print(tag+": "+s);
        e.printStackTrace();
    }

    public static void w(String tag, Throwable e) {
        System.err.print(tag);
        e.printStackTrace();
    }
}
