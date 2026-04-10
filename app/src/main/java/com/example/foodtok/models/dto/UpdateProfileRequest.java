package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Request body for PATCH {@code /rest/v1/profiles} to update a user's
 * avatar, preference tags, and allergen blacklist after onboarding.
 */
public class UpdateProfileRequest {

  @SerializedName("avatar_url")
  public String avatarUrl;

  @SerializedName("interest_profile")
  public Map<String, Integer> interestProfile;

  @SerializedName("blacklisted_ingredients")
  public List<String> blacklistedIngredients;
}