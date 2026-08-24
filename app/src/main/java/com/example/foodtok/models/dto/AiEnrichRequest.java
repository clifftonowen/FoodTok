package com.example.foodtok.models.dto;

import com.example.foodtok.models.Recipe;

/** Request body for the {@code gemini-enrich} Supabase Edge Function. */
public class AiEnrichRequest {

  private final AiRecipeContext recipe;

  private AiEnrichRequest(AiRecipeContext recipe) {
    this.recipe = recipe;
  }

  /**
   * Builds an enrichment request for a single recipe.
   *
   * @param recipe the recipe to analyse
   * @return a serialisable request body
   */
  public static AiEnrichRequest create(Recipe recipe) {
    return new AiEnrichRequest(AiRecipeContext.from(recipe));
  }
}
