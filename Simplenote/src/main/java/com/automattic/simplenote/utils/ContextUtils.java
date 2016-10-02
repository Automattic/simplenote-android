package com.automattic.simplenote.utils;

import android.content.Context;
import android.support.annotation.RawRes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ContextUtils {
    public static String readRawResourceFile(Context context, @RawRes int resId) {
        InputStream stream = null;
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        String line;

        try {
            stream = context.getResources().openRawResource(resId);
            reader = new BufferedReader(new InputStreamReader(stream));

            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }

            return builder.toString();
        } catch (IOException ex) {
            return null;
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ignored) {
            }

            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ignored) {
            }
        }
    }
}
