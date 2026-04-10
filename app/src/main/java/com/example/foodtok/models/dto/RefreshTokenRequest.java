package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

/** Request body for Supabase token refresh ({@code grant_type=refresh_token}). */
public class RefreshTokenRequest {

  @SerializedName("refresh_token")
  public final String refreshToken;

  public RefreshTokenRequest(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}