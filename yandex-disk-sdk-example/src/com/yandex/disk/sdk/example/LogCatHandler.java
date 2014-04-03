/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.sdk.example;

import android.app.Application;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogCatHandler extends Handler {

    private static final String TAG = "LogCatHandler";

    private static String anonymous;

    /**
     * File called logging.properties must be in the same package as application.
     * 
     * @param application
     */
    public static void setup(Application application) {
        try {
            anonymous = application.getPackageName();
            LogManager manager = LogManager.getLogManager();
            InputStream config = application.getClass().getResourceAsStream("logging.properties");
            if (config != null) {
                manager.readConfiguration(config);
                Logger logger = manager.getLogger("");
                logger.addHandler(new LogCatHandler());
                logger.fine("configuration read");
            } else {
                Log.w(TAG, "configuring failed");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error read configuration", e);
        }
    }

    private static final Formatter THE_FORMATTER = new Formatter() {

        @Override
        public String format(LogRecord r) {
            Throwable thrown = r.getThrown();
            if (thrown != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                sw.write(r.getMessage());
                sw.write("\n");
                thrown.printStackTrace(pw);
                pw.flush();
                return sw.toString();
            } else {
                return r.getMessage();
            }
        }

    };

    public LogCatHandler () {
        setFormatter(THE_FORMATTER);
    }

    @Override
    public void close() {
        // No need to close, but must implement abstract method.
    }

    @Override
    public void flush() {
        // No need to flush, but must implement abstract method.
    }

    @Override
    public void publish(LogRecord record) {
        try {
            int level = getAndroidLevel(record.getLevel());
            String tag = record.getLoggerName();

            if (tag == null) {
                // Anonymous logger.
                tag = anonymous;
            } else {
                // Tags must be <= 23 characters.
                int length = tag.length();
                if (length > 23) {
                    // Most loggers use the full class name. Try dropping the
                    // package.
                    int lastPeriod = tag.lastIndexOf(".");
                    if (length - lastPeriod - 1 <= 23) {
                        tag = tag.substring(lastPeriod + 1);
                    } else {
                        // Use last 23 chars.
                        tag = tag.substring(tag.length() - 23);
                    }
                }
            }

            String message = getFormatter().format(record);
            Log.println(level, tag, message);
        } catch (RuntimeException e) {
            Log.e(TAG, "Error logging message.", e);
        }
    }

    /**
     * Converts a {@link java.util.logging.Logger} logging level into an Android one.
     * 
     * @param level
     *            The {@link java.util.logging.Logger} logging level.
     * 
     * @return The resulting Android logging level.
     */
    private static int getAndroidLevel(Level level) {
        int value = level.intValue();
        if (value >= 1000) { // SEVERE
            return Log.ERROR;
        } else if (value >= 900) { // WARNING
            return Log.WARN;
        } else if (value >= 800) { // INFO
            return Log.INFO;
        } else {
            return Log.DEBUG;
        }
    }

}
