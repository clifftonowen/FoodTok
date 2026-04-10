package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

/** DTO for the Supabase {@code profiles} table, used in PostgREST joins. */
public class UserDto {

  @SerializedName("id")
  public String id;

  @SerializedName("username")
  public String username;

  @SerializedName("avatar_url")
  public String avatarUrl;
}
