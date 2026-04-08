package com.example.foodtok.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds AI-generated metadata for a recipe.
 * Populated by GeminiEnrichmentService; displayed with "AI Generated" labels.
 *
 * OOP: Encapsulation — all fields private, unmodifiable lists returned.
 * Tell, Don't Ask — query methods like hasAllergenWarnings() encapsulate logic.
 */
public class RecipeEnrichment {

    private final String recipeId;
    private final List<String> detectedAllergens;
    private final List<String> generatedInstructions;
    private final double estimatedCalories;
    private final List<String> suggestedTags;
    private final long generatedAtMillis;

    public RecipeEnrichment(String recipeId,
                            List<String> detectedAllergens,
                            List<String> generatedInstructions,
                            double estimatedCalories,
                            List<String> suggestedTags) {
        this.recipeId = recipeId;
        this.detectedAllergens = detectedAllergens != null
                ? new ArrayList<>(detectedAllergens) : new ArrayList<>();
        this.generatedInstructions = generatedInstructions != null
                ? new ArrayList<>(generatedInstructions) : new ArrayList<>();
        this.estimatedCalories = estimatedCalories;
        this.suggestedTags = suggestedTags != null
                ? new ArrayList<>(suggestedTags) : new ArrayList<>();
        this.generatedAtMillis = System.currentTimeMillis();
    }

    // --- Query methods (Tell, Don't Ask) ---

    public boolean hasAllergenWarnings() {
        return !detectedAllergens.isEmpty();
    }

    /**
     * Checks if a specific ingredient was flagged as an allergen by AI.
     * Case-insensitive comparison.
     */
    public boolean isIngredientFlaggedByAI(String ingredientName) {
        if (ingredientName == null) return false;
        String lower = ingredientName.toLowerCase();
        for (String allergen : detectedAllergens) {
            if (allergen.toLowerCase().equals(lower)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasGeneratedInstructions() {
        return !generatedInstructions.isEmpty();
    }

    public boolean hasSuggestedTags() {
        return !suggestedTags.isEmpty();
    }

    public boolean hasEstimatedCalories() {
        return estimatedCalories > 0;
    }

    // --- Getters (unmodifiable lists to prevent external mutation) ---

    public String getRecipeId() { return recipeId; }

    public List<String> getDetectedAllergens() {
        return Collections.unmodifiableList(detectedAllergens);
    }

    public List<String> getGeneratedInstructions() {
        return Collections.unmodifiableList(generatedInstructions);
    }

    public double getEstimatedCalories() { return estimatedCalories; }

    public List<String> getSuggestedTags() {
        return Collections.unmodifiableList(suggestedTags);
    }

    public long getGeneratedAtMillis() { return generatedAtMillis; }
}
