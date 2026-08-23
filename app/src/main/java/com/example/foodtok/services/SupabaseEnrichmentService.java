package com.example.foodtok.services;

import androidx.annotation.NonNull;

import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.RecipeEnrichment;
import com.example.foodtok.models.dto.AiEnrichRequest;
import com.example.foodtok.models.dto.AiTextResponse;
import com.example.foodtok.util.ApiClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Real implementation of {@link IRecipeEnrichmentService}, backed by the
 * {@code gemini-enrich} Supabase Edge Function.
 *
 * <p>The function returns the model's raw text rather than a parsed object, so the JSON
 * parsing and per-recipe caching below are unchanged from the previous on-device Gemini
 * implementation.
 *
 * <p>OOP: implements IRecipeEnrichmentService (polymorphism). Encapsulation: the cache and
 * parsing logic are private.
 */
public class SupabaseEnrichmentService implements IRecipeEnrichmentService {

  private final AiProxyApi api;
  private final Map<String, RecipeEnrichment> cache;

  public SupabaseEnrichmentService() {
    this.cache = new HashMap<>();
    this.api = ApiClient.getFunctionsClient().create(AiProxyApi.class);
  }

  @Override
  public void enrichRecipe(Recipe recipe, EnrichmentCallback callback) {
    String recipeId = recipe.getId();

    // Return cached result if available
    if (cache.containsKey(recipeId)) {
      callback.onEnriched(cache.get(recipeId));
      return;
    }

    AiEnrichRequest request = AiEnrichRequest.create(recipe);

    api.enrich(request).enqueue(new Callback<AiTextResponse>() {
      @Override
      public void onResponse(@NonNull Call<AiTextResponse> call,
                                   @NonNull Response<AiTextResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
          String text = response.body().getText();
          if (text != null) {
            RecipeEnrichment enrichment = parseEnrichment(recipeId, text);
            if (enrichment != null) {
              cache.put(recipeId, enrichment);
              callback.onEnriched(enrichment);
            } else {
              callback.onError("Failed to parse enrichment response");
            }
          } else {
            callback.onError("Empty response from the AI service");
          }
        } else if (response.code() == 429) {
          callback.onError("Too many requests — please wait a moment before trying again.");
        } else {
          callback.onError("AI service error: " + response.code());
        }
      }

      @Override
      public void onFailure(@NonNull Call<AiTextResponse> call,
                 @NonNull Throwable t) {
        callback.onError("Network error: " + t.getMessage());
      }
    });
  }

  @Override
  public RecipeEnrichment getCachedEnrichment(String recipeId) {
    return cache.get(recipeId);
  }

  @Override
  public boolean isEnriched(String recipeId) {
    return cache.containsKey(recipeId);
  }

  // --- Private helpers ---

  /**
   * Parses the JSON response into a RecipeEnrichment object.
   * Handles potential variations in the response format.
   */
  private RecipeEnrichment parseEnrichment(String recipeId, String jsonText) {
    try {
      // Strip markdown code fences if the model adds them despite instructions
      String cleaned = jsonText.trim();
      if (cleaned.startsWith("```")) {
        cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").trim();
      }

      JsonObject json = JsonParser.parseString(cleaned).getAsJsonObject();

      List<String> allergens = parseStringArray(json, "detected_allergens");
      List<String> instructions = parseStringArray(json, "instructions");
      double calories = json.has("estimated_calories")
          ? json.get("estimated_calories").getAsDouble() : 0;
      List<String> tags = parseStringArray(json, "suggested_tags");

      return new RecipeEnrichment(recipeId, allergens, instructions, calories, tags);
    } catch (Exception e) {
      return null;
    }
  }

  private List<String> parseStringArray(JsonObject json, String key) {
    List<String> result = new ArrayList<>();
    if (json.has(key) && json.get(key).isJsonArray()) {
      JsonArray array = json.getAsJsonArray(key);
      for (JsonElement element : array) {
        result.add(element.getAsString());
      }
    }
    return result;
  }
}
