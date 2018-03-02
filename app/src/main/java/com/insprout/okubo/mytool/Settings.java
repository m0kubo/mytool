package com.insprout.okubo.mytool;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

/**
 * Created by okubo on 2018/02/09.
 */

public class Settings {

    private final static String KEY_ADJUST_RATE = "settings.ADJUST_RATE";
    private final static String KEY_FILE_PATH = "viewer.FILE_PATH";
    private final static String KEY_FILE_SIZE = "viewer.FONT_SIZE";


    public static int getAdjustRate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_ADJUST_RATE, 0);
    }

    public static void putAdjustRate(Context context, int value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(KEY_ADJUST_RATE, value).apply();
    }


    public static float getFontSize(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getFloat(KEY_FILE_SIZE, 0);
    }

    public static void putFontSize(Context context, float fontSize) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putFloat(KEY_FILE_SIZE, fontSize).apply();
    }

    public static Uri getFileUri(Context context) {
        String strUri = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_FILE_PATH, null);
        if (strUri == null) return null;
        return Uri.parse(strUri);
    }

    public static void putFileUri(Context context, Uri uri) {
        String strUri = null;
        if (uri != null) strUri = uri.toString();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(KEY_FILE_PATH, strUri).apply();
    }

}
