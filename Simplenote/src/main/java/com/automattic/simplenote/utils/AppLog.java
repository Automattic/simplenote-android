package com.automattic.simplenote.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class AppLog {
    private static final int LOG_MAX = 100;

    private static final LinkedHashMap<Integer, String> mQueue  = new LinkedHashMap<Integer, String>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
            return this.size() > LOG_MAX;
        }
    };

    public enum Type {
        ACCOUNT,
        ACTION,
        AUTH,
        DEVICE,
        LAYOUT,
        NETWORK,
        SCREEN,
        SYNC,
        IMPORT
    }

    public static void add(Type type, String message) {
        String log;

        if (type == Type.ACCOUNT || type == Type.DEVICE) {
            log = message + "\n";
        } else {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
            log = timestamp + " - " + type.toString() + ": " + message + "\n";
        }

        mQueue.put(mQueue.size(), log);
    }

    public static String get() {
        StringBuilder queue = new StringBuilder();

        for (Map.Entry<Integer, String> entry : mQueue.entrySet()) {
            queue.append(entry.getValue());
        }

        return queue.toString();
    }
}
