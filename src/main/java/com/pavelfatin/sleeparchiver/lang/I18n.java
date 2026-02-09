package com.pavelfatin.sleeparchiver.lang;

import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {
    private static final String BUNDLE = "com.pavelfatin.sleeparchiver.messages";
    private static ResourceBundle bundle;

    static {
        reload();
    }

    public static void reload() {
        bundle = ResourceBundle.getBundle(BUNDLE, Locale.getDefault());
    }

    public static String t(String key) {
        return bundle.getString(key);
    }

    public static String t(String key, Object... args) {
        return String.format(bundle.getString(key), args);
    }
}
