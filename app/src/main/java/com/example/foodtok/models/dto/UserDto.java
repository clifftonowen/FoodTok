package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/** DTO for the Supabase {@code profiles} table, used in PostgREST joins. */
public class UserDto {

  @SerializedName("id")
  public String id;

  @SerializedName("username")
  public String username;

  @SerializedName("avatar_url")
  public String avatarUrl;

  @SerializedName("bio")
  public String bio;
    @SerializedName("interest_profile")
    public Map<String, Integer> interestProfile;

    @SerializedName("blacklisted_ingredients")
    public List<String> blacklistedIngredients;
    }
