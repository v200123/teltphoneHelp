package com.u2tzjtne.telephonehelper.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.u2tzjtne.telephonehelper.base.App;

/**
 * SharedPreferences工具类
 *
 * @author u2tzjtne@gmail.com
 * @date 2017/10/9
 */
public class SPUtils {

    private static SharedPreferences sp = App.getContext()
            .getSharedPreferences("APP_DATA", Context.MODE_PRIVATE);
    private static SharedPreferences.Editor edit = sp.edit();

    public static void putBoolean(String key, boolean value) {
        edit.putBoolean(key, value);
        edit.apply();
    }

    public static boolean getBoolean(String key, boolean defValue) {
        return sp.getBoolean(key, defValue);
    }

    public static void putInt(String key, int value) {
        edit.putInt(key, value);
        edit.apply();
    }

    public static int getInt(String key, int defValue) {
        return sp.getInt(key, defValue);
    }

    public static void putString(String key, String value) {
        edit.putString(key, value);
        edit.apply();
    }

    public static String getString(String key, String defValue) {
        return sp.getString(key, defValue);
    }
}
