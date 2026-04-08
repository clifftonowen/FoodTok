package com.example.foodtok.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static Retrofit restRetrofit;   // for table queries
    private static Retrofit authRetrofit;   // for signup/login

    private ApiClient() {
        // prevent instantiation
    }

    // Shared OkHttpClient — attaches headers to EVERY request
    private static OkHttpClient buildClient() {
        // Logging — shows request/response in Logcat (debug only)
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request.Builder builder = chain.request().newBuilder()
                            // Every Supabase request needs the anon key
                            .addHeader("apikey", Constants.SUPABASE_ANON_KEY)
                            .addHeader("Content-Type", "application/json");

                    // If user is logged in, attach their JWT
                    String token = SessionManager.getInstance().getAccessToken();
                    if (token != null) {
                        builder.addHeader("Authorization", "Bearer " + token);
                    }

                    // PostgREST: return the created/updated row in response
                    builder.addHeader("Prefer", "return=representation");

                    return chain.proceed(builder.build());
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
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return authRetrofit;
    }
}