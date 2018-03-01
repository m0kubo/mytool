package com.insprout.okubo.mytool;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by okubo on 2018/02/09.
 */

public class Settings {

    private final static String KEY_ADJUST_RATE = "settings.ADJUST_RATE";

    public static int getAdjustRate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_ADJUST_RATE, 0);
    }

    public static void putAdjustRate(Context context, int value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(KEY_ADJUST_RATE, value).apply();
    }

}
