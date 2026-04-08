package com.example.foodtok.services;

import com.example.foodtok.models.Ingredient;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.RecipeEnrichment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock implementation of IRecipeEnrichmentService for testing without a Gemini API key.
 * Returns hardcoded enrichment data based on the recipe's ingredients.
 *
 * Follows the same pattern as MockChatService and MockInteractionService.
 */
public class MockEnrichmentService implements IRecipeEnrichmentService {

    private final Map<String, RecipeEnrichment> cache = new HashMap<>();

    @Override
    public void enrichRecipe(Recipe recipe, EnrichmentCallback callback) {
        String recipeId = recipe.getId();

        if (cache.containsKey(recipeId)) {
            callback.onEnriched(cache.get(recipeId));
            return;
        }

        // Detect allergens from ingredients that have the allergen flag
        List<String> detectedAllergens = new ArrayList<>();
        for (Ingredient ing : recipe.getIngredients()) {

            String name = ing.getName();
            detectedAllergens.add(name.substring(0, 1).toUpperCase() + name.substring(1));

        }

        // Generate mock instructions
        List<String> instructions = Arrays.asList(
                "Prepare all ingredients as listed.",
                "Combine ingredients following standard cooking techniques.",
                "Cook until done. Adjust seasoning to taste.",
                "Plate and serve immediately."
        );

        // Use recipe's own calorie estimate or a mock value
        double calories = recipe.getEstimatedCalories() > 0
                ? recipe.getEstimatedCalories() : 350;

        // Mock suggested tags
        List<String> tags = Arrays.asList("Homemade", "Easy");

        RecipeEnrichment enrichment = new RecipeEnrichment(
                recipeId, detectedAllergens, instructions, calories, tags);
        cache.put(recipeId, enrichment);

        callback.onEnriched(enrichment);
    }

    @Override
    public RecipeEnrichment getCachedEnrichment(String recipeId) {
        return cache.get(recipeId);
    }

    @Override
    public boolean isEnriched(String recipeId) {
        return cache.containsKey(recipeId);
    }
}
