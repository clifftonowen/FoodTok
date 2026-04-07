package com.example.foodtok.auth;

public final class AuthServiceProvider {

    private static IAuthService authService;

    private AuthServiceProvider() {
        // prevent instantiation
    }

    public static IAuthService getAuthService() {
        if (authService == null) {
            authService = new MockAuthService();
        }
        return authService;
    }

    public static void setAuthService(IAuthService service) {
        authService = service;
    }
}