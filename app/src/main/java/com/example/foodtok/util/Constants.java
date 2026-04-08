package com.example.foodtok.util;

import com.example.foodtok.BuildConfig;

public final class Constants {

    private Constants() {
        // prevent instantiation
    }

    // Base URL from local.properties via BuildConfig
    public static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    public static final String SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;

    // PostgREST endpoints (CRUD for tables)
    public static final String REST_BASE_URL = SUPABASE_URL + "/rest/v1/";

    // Auth endpoints (signup, login, logout)
    public static final String AUTH_BASE_URL = SUPABASE_URL + "/auth/v1/";

    // Edge Functions (Gemini chatbot later)
    public static final String FUNCTIONS_BASE_URL = SUPABASE_URL + "/functions/v1/";
}