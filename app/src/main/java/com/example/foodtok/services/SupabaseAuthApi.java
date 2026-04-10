package com.example.foodtok.services;

import com.example.foodtok.models.dto.AuthResponse;
import com.example.foodtok.models.dto.LoginRequest;
import com.example.foodtok.models.dto.RefreshTokenRequest;
import com.example.foodtok.models.dto.SignUpRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** Retrofit interface for Supabase GoTrue authentication endpoints. */
public interface SupabaseAuthApi {

  // POST /auth/v1/signup
  @POST("signup")
  Call<AuthResponse> signUp(@Body SignUpRequest request);

  // POST /auth/v1/token?grant_type=password
  @POST("token")
  Call<AuthResponse> login(
      @Query("grant_type") String grantType,
      @Body LoginRequest request
  );

  // POST /auth/v1/token?grant_type=refresh_token
  @POST("token")
  Call<AuthResponse> refreshToken(
      @Query("grant_type") String grantType,
      @Body RefreshTokenRequest request
  );

  // POST /auth/v1/logout
  @POST("logout")
  Call<Void> logout(@Header("Authorization") String token);
}