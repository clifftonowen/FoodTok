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

  // Storage (video/image upload)
  public static final String STORAGE_BASE_URL = SUPABASE_URL + "/storage/v1/";

  // Edge Functions (Gemini chat + enrichment proxy)
  public static final String FUNCTIONS_BASE_URL = SUPABASE_URL + "/functions/v1/";

  /**
   * Whether this build is pointed at a Supabase project, and therefore whether the
   * server-side AI proxy is reachable.
   *
   * <p>Replaces the old "is a Gemini key baked into this APK?" check. The key now lives
   * as a Supabase secret, so the honest question is whether we have a backend at all.
   *
   * @return true when both the Supabase URL and anon key are configured
   */
  public static boolean isAiProxyConfigured() {
    return SUPABASE_URL != null && !SUPABASE_URL.isEmpty()
        && SUPABASE_ANON_KEY != null && !SUPABASE_ANON_KEY.isEmpty();
  }
}