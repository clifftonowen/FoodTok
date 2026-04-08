package com.example.foodtok.services;

import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.RecipeEnrichment;

/**
 * Contract for AI-powered recipe enrichment.
 * Analyzes a recipe and generates missing metadata:
 * - Allergen detection (flags ingredients the uploader missed)
 * - Instruction generation (step-by-step from recipe context)
 * - Calorie estimation
 * - Tag suggestions
 *
 * Results are cached per recipe to avoid redundant API calls.
 *
 * Implementations: GeminiEnrichmentService (real), MockEnrichmentService (testing).
 */
public interface IRecipeEnrichmentService {

    /**
     * Enriches a recipe with AI-generated metadata.
     * If the recipe has already been enriched, returns the cached result immediately.
     */
    void enrichRecipe(Recipe recipe, EnrichmentCallback callback);

    /**
     * Returns the cached enrichment for a recipe, or null if not yet enriched.
     */
    RecipeEnrichment getCachedEnrichment(String recipeId);

    /**
     * Returns whether this recipe has been enriched already.
     */
    boolean isEnriched(String recipeId);
}
