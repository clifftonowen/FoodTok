package com.example.foodtok;

import android.app.Application;

import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.auth.AuthServiceProvider;
import com.example.foodtok.auth.SupabaseAuthService;
import com.example.foodtok.models.User;
import com.example.foodtok.util.SessionManager;

public class FoodTokApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Initialize SessionManager (needs Context, so must be first)
        SessionManager.init(this);

        // 2. Set real auth service (replaces MockAuthService)
        AuthServiceProvider.setAuthService(new SupabaseAuthService());

        // 3. Restore login state if user was previously logged in
        restoreSession();
    }

    private void restoreSession() {
        SessionManager session = SessionManager.getInstance();

        if (session.isLoggedIn()) {
            // User was logged in before app closed
            // Create a basic User object from saved ID
            // Full profile can be fetched later from Supabase
            String userId = session.getUserId();
            String username = session.getUsername();
            User user = new User(userId, username, "");
            AuthManager.getInstance().login(user);
        }
    }
}