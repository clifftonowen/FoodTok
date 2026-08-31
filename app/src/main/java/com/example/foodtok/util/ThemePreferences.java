package com.example.foodtok.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/** Persists and applies the user's app appearance preference. */
public final class ThemePreferences {

  private static final String PREFS_NAME = "foodtok_appearance";
  private static final String KEY_DARK_MODE = "dark_mode";
  private static final boolean DEFAULT_DARK_MODE = true;

  private ThemePreferences() {}

  public static boolean isDarkMode(Context context) {
    return preferences(context).getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE);
  }

  public static void applySavedTheme(Context context) {
    applyMode(isDarkMode(context));
  }

  public static void setDarkMode(Context context, boolean enabled) {
    preferences(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    applyMode(enabled);
  }

  private static void applyMode(boolean darkMode) {
    AppCompatDelegate.setDefaultNightMode(darkMode
        ? AppCompatDelegate.MODE_NIGHT_YES
        : AppCompatDelegate.MODE_NIGHT_NO);
  }

  private static SharedPreferences preferences(Context context) {
    return context.getApplicationContext()
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }
}
