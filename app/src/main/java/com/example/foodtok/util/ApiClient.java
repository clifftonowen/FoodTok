package com.example.foodtok.util;

import android.util.Log;

import com.example.foodtok.models.dto.AuthResponse;
import com.example.foodtok.models.dto.RefreshTokenRequest;
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.services.SupabaseAuthApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Singleton Retrofit client factory for Supabase REST, Auth, and Storage APIs. */
public final class ApiClient {

  private static Retrofit restRetrofit;   // for table queries
  private static Retrofit authRetrofit;   // for signup/login
  private static Retrofit storageRetrofit; // for file upload
  private static Retrofit functionsRetrofit; // for Edge Functions (AI proxy)
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
    if (PreviewMode.isEnabled()) {
      return null;
    }
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

    OkHttpClient.Builder client = new OkHttpClient.Builder();
    addPreviewBlocker(client);
    return client
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

    OkHttpClient.Builder client = new OkHttpClient.Builder();
    addPreviewBlocker(client);
    return client
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

  /**
   * OkHttpClient for Supabase Edge Functions.
   *
   * <p>Differs from {@link #buildClient()} in two deliberate ways:
   *
   * <ul>
   *   <li>No {@code Prefer} header - that is PostgREST-specific and meaningless here.
   *   <li>The anon key is used as a bearer token when no user is logged in. The functions
   *       run with {@code verify_jwt = true}, so a guest with no Authorization header
   *       would get a hard 401 - and the feed and chat pages are reachable without
   *       logging in, which is exactly the public-demo case this must support.
   * </ul>
   */
  private static OkHttpClient buildFunctionsClient() {
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    logging.setLevel(HttpLoggingInterceptor.Level.BODY);

    OkHttpClient.Builder client = new OkHttpClient.Builder()
        .addInterceptor(chain -> {
          Request.Builder builder = chain.request().newBuilder()
              .addHeader("apikey", Constants.SUPABASE_ANON_KEY)
              .addHeader("Content-Type", "application/json");

          String token = SessionManager.getInstance().getAccessToken();
          builder.addHeader("Authorization",
              "Bearer " + (token != null ? token : Constants.SUPABASE_ANON_KEY));

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
        .addInterceptor(logging);

    addPreviewBlocker(client);
    return client.build();
  }

  /** Returns a local synthetic response before DNS/socket work in preview mode. */
  private static void addPreviewBlocker(OkHttpClient.Builder client) {
    if (!PreviewMode.isEnabled()) {
      return;
    }
    client.addInterceptor(chain -> new okhttp3.Response.Builder()
        .request(chain.request())
        .protocol(Protocol.HTTP_1_1)
        .code(503)
        .message("Offline preview")
        .body(ResponseBody.create(
            "{\"message\":\"Offline preview\"}",
            MediaType.get("application/json")))
        .build());
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

  /** Retrofit instance for the Supabase Edge Functions that proxy Gemini. */
  public static Retrofit getFunctionsClient() {
    if (functionsRetrofit == null) {
      functionsRetrofit = new Retrofit.Builder()
          .baseUrl(Constants.FUNCTIONS_BASE_URL)
          .client(buildFunctionsClient())
          .addConverterFactory(GsonConverterFactory.create())
          .build();
    }
    return functionsRetrofit;
  }

  /** Convenience accessor for the PostgREST API interface. */
  public static SupabaseApi getSupabaseApi() {
    if (supabaseApi == null) {
      supabaseApi = getRestClient().create(SupabaseApi.class);
    }
    return supabaseApi;
  }
}
