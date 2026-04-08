package com.example.foodtok.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "foodtok_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ID = "user_id";

    private static SessionManager instance;
    private final SharedPreferences prefs;

    // Private constructor — only init() can create this
    private SessionManager(Context context) {
        // MODE_PRIVATE = only this app can read it
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Call this ONCE in Application.onCreate()
    public static void init(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "SessionManager not initialized. Call init(context) first.");
        }
        return instance;
    }

    // Save tokens after successful login/signup
    public void saveSession(String accessToken, String refreshToken, String userId, String userName) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USERNAME,userName)
                .apply();  // apply() is async, commit() is sync
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }


    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    // Clear everything on logout
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}