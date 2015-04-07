package com.automattic.simplenote.utils;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by richard on 9/7/14.
 */
public final class IntentUtil {

    public static final String TAG = "IntentUtil";
    
    public static void dump(Intent i){
        Log.i(TAG, "intent: " + i.toString());
        Log.i(TAG, "action: " + i.getAction());
        Log.i(TAG, "package: " + i.getPackage());

        Bundle b = i.getExtras();
        if (b == null || b.size() == 0){
            Log.i(TAG, "no extras");
            return;
        }

        for (String k : b.keySet()){
            Log.i(TAG, new StringBuilder().append("key ")
                .append(k)
                .append("=")
                .append(b.get(k).toString())
                .toString());

        }
    }
}
