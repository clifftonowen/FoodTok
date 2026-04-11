package com.example.foodtok.util;

import android.util.Log;

import com.example.foodtok.models.dto.AuthResponse;
import com.example.foodtok.models.dto.RefreshTokenRequest;
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.services.SupabaseAuthApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Singleton Retrofit client factory for Supabase REST, Auth, and Storage APIs. */
public final class ApiClient {

  private static Retrofit restRetrofit;   // for table queries
  private static Retrofit authRetrofit;   // for signup/login
  private static Retrofit storageRetrofit; // for file upload
  private static SupabaseApi supabaseApi; // cached instance

  private static final Gson GSON = new GsonBuilder()
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
      .create();

  private static final String TAG = "ApiClient";

  private ApiClient() {
    // prevent instantiation
  }

  /**
   * Attempts to refresh the Supabase JWT using the stored refresh token.
   * On success, persists the new tokens and returns the new access token.
   * Returns {@code null} on failure (caller should redirect to login).
   */
  private static synchronized String tryRefreshToken() {
    String refreshToken =
        SessionManager.getInstance().getRefreshToken();
    if (refreshToken == null) {
      return null;
    }

    try {
      Retrofit authRetrofit = new Retrofit.Builder()
          .baseUrl(Constants.AUTH_BASE_URL)
          .client(new OkHttpClient.Builder()
              .addInterceptor(chain -> chain.proceed(
                  chain.request().newBuilder()
                      .addHeader("apikey", Constants.SUPABASE_ANON_KEY)
                      .addHeader("Content-Type", "application/json")
                      .build()))
              .build())
          .addConverterFactory(GsonConverterFactory.create(GSON))
          .build();

      SupabaseAuthApi authApi =
          authRetrofit.create(SupabaseAuthApi.class);
      Response<AuthResponse> response = authApi
          .refreshToken("refresh_token",
              new RefreshTokenRequest(refreshToken))
          .execute();

      if (response.isSuccessful() && response.body() != null) {
        AuthResponse body = response.body();
        SessionManager.getInstance().saveSession(
            body.getAccessToken(),
            body.getRefreshToken(),
            SessionManager.getInstance().getUserId(),
            SessionManager.getInstance().getUsername());
        Log.d(TAG, "JWT refreshed successfully");
        return body.getAccessToken();
      } else {
        Log.w(TAG, "Token refresh failed: " + response.code());
        return null;
      }
    } catch (Exception e) {
      Log.w(TAG, "Token refresh error", e);
      return null;
    }
  }

  // Shared OkHttpClient — attaches headers to EVERY request
  private static OkHttpClient buildClient() {
    // Logging — shows request/response in Logcat (debug only)
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    logging.setLevel(HttpLoggingInterceptor.Level.BODY);

    return new OkHttpClient.Builder()
        .addInterceptor(chain -> {
          Request original = chain.request();
          Request.Builder builder = original.newBuilder()
              // Every Supabase request needs the anon key
              .addHeader("apikey", Constants.SUPABASE_ANON_KEY)
              .addHeader("Content-Type", "application/json");

          // If user is logged in, attach their JWT
          String token = SessionManager.getInstance().getAccessToken();
          if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
          }

          // PostgREST Prefer header: default to return=representation, but
          // let the caller override (e.g. upsert needs to add
          // resolution=merge-duplicates). Using header() replaces rather
          // than appends, so we don't end up with two Prefer headers.
          if (original.header("Prefer") == null) {
            builder.header("Prefer", "return=representation");
          }

          return chain.proceed(builder.build());
        })
        .authenticator((route, response) -> {
          // OkHttp calls this on 401 — try refreshing the JWT
          String newToken = tryRefreshToken();
          if (newToken == null) {
            return null; // give up, let the 401 propagate
          }
          return response.request().newBuilder()
              .header("Authorization", "Bearer " + newToken)
              .build();
        })
        .addInterceptor(logging)
        .build();
  }

  /**
   * OkHttpClient for storage uploads — no Content-Type header
   * (multipart sets its own) and no Prefer header.
   */
  private static OkHttpClient buildStorageClient() {
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    logging.setLevel(HttpLoggingInterceptor.Level.BODY);

    return new OkHttpClient.Builder()
        .addInterceptor(chain -> {
          Request.Builder builder = chain.request().newBuilder()
              .addHeader("apikey", Constants.SUPABASE_ANON_KEY);

          String token = SessionManager.getInstance().getAccessToken();
          if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
          }

          return chain.proceed(builder.build());
        })
        .authenticator((route, response) -> {
          String newToken = tryRefreshToken();
          if (newToken == null) {
            return null;
          }
          return response.request().newBuilder()
              .header("Authorization", "Bearer " + newToken)
              .build();
        })
        .addInterceptor(logging)
        .build();
  }

  // Retrofit instance for table CRUD (recipes, profiles, follows, etc.)
  public static Retrofit getRestClient() {
    if (restRetrofit == null) {
      restRetrofit = new Retrofit.Builder()
          .baseUrl(Constants.REST_BASE_URL)
          .client(buildClient())
          .addConverterFactory(GsonConverterFactory.create())
          .build();
    }
    return restRetrofit;
  }

  // Retrofit instance for Auth (signup, login, logout)
  public static Retrofit getAuthClient() {
    if (authRetrofit == null) {
      authRetrofit = new Retrofit.Builder()
          .baseUrl(Constants.AUTH_BASE_URL)
          .client(buildClient())
          .addConverterFactory(GsonConverterFactory.create(GSON))
          .build();
    }
    return authRetrofit;
  }

  // Retrofit instance for Supabase Storage (video/image upload)
  public static Retrofit getStorageClient() {
    if (storageRetrofit == null) {
      storageRetrofit = new Retrofit.Builder()
          .baseUrl(Constants.STORAGE_BASE_URL)
          .client(buildStorageClient())
          .addConverterFactory(GsonConverterFactory.create(GSON))
          .build();
    }
    return storageRetrofit;
  }

  /** Convenience accessor for the PostgREST API interface. */
  public static SupabaseApi getSupabaseApi() {
    if (supabaseApi == null) {
      supabaseApi = getRestClient().create(SupabaseApi.class);
    }
    return supabaseApi;
  }
}
