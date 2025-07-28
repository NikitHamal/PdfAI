package com.pdf.ai;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class ThemeManager {
    
    public static final String THEME_PREFERENCE_KEY = "theme_preference";
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    
    public static void applyTheme(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = prefs.getString(THEME_PREFERENCE_KEY, THEME_SYSTEM);
        
        switch (theme) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
    
    public static void setTheme(Context context, String theme) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(THEME_PREFERENCE_KEY, theme).apply();
        
        if (context instanceof Activity) {
            ((Activity) context).recreate();
        }
    }
    
    public static boolean isDarkTheme(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = prefs.getString(THEME_PREFERENCE_KEY, THEME_SYSTEM);
        
        if (THEME_DARK.equals(theme)) {
            return true;
        } else if (THEME_LIGHT.equals(theme)) {
            return false;
        } else {
            // Follow system theme
            return AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
        }
    }
}