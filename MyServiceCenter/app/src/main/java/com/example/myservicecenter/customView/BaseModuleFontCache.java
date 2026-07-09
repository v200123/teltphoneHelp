package com.example.myservicecenter.customView;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;

public class BaseModuleFontCache {
    private static HashMap<String, Typeface> fontCache = new HashMap<>();

    public static Typeface getTypeface(String str, Context context) {
        Typeface typefaceCreateFromAsset = fontCache.get(str);
        if (typefaceCreateFromAsset == null) {
            try {
                typefaceCreateFromAsset = Typeface.createFromAsset(context.getAssets(), str);
                fontCache.put(str, typefaceCreateFromAsset);
            } catch (Exception unused) {
                return null;
            }
        }
        return typefaceCreateFromAsset;
    }
}
