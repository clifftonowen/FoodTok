package com.example.foodtok.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Recipe {
    private String id;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private String authorId;
    private String authorName;
    private List<String> tags;
    private List<Ingredient> ingredients;
    private int prepTimeMinutes;
    private int cookTimeMinutes;
    private double estimatedCalories;

    public Recipe(String id, String title, String videoUrl, List<String> tags, List<Ingredient> ingredients) {
        this.id = id;
        this.title = title;
        this.videoUrl = videoUrl;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.ingredients = ingredients != null ? new ArrayList<>(ingredients) : new ArrayList<>();
    }

    // --- Behavior methods (Tell, Don't Ask) ---

    /**
     * Sums calories across all ingredients.
     * Used for nutritional info display on recipe detail page.
     */
    public double calculateCalories() {
        double total = 0;
        for (Ingredient ingredient : ingredients) {
            total += ingredient.getCalories();
        }
        return total;
    }

    /**
     * Checks if this recipe contains any ingredient in the user's blacklist.
     * Used by AllergenService before scoring for the feed.
     */
    public boolean containsAllergen(Set<String> blacklistedIngredients) {
        for (Ingredient ingredient : ingredients) {
            if (blacklistedIngredients.contains(ingredient.getName().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this recipe can be made with the given available ingredients.
     * Used by ingredient-based search.
     */
    public boolean canMakeWith(Set<String> availableIngredients) {
        for (Ingredient ingredient : ingredients) {
            if (!availableIngredients.contains(ingredient.getName().toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns number of matching ingredients the user has.
     * Useful for partial-match ranking in search.
     */
    public int countMatchingIngredients(Set<String> availableIngredients) {
        int count = 0;
        for (Ingredient ingredient : ingredients) {
            if (availableIngredients.contains(ingredient.getName().toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    // --- Getters (no setters for id, tags, ingredients — use constructor or dedicated methods) ---

    public String getId() { return id; }

    public String getTitle() { return title; }

    public String getDescription() { return description; }

    public String getVideoUrl() { return videoUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }

    public String getAuthorId() { return authorId; }

    public String getAuthorName() { return authorName; }

    public int getPrepTimeMinutes() { return prepTimeMinutes; }

    public int getCookTimeMinutes() { return cookTimeMinutes; }

    public double getEstimatedCalories() { return estimatedCalories; }

    /**
     * Returns an unmodifiable view — callers cannot accidentally mutate the list.
     */
    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public List<Ingredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    // --- Setters ---

    public void setDescription(String description) { this.description = description; }

    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public void setPrepTimeMinutes(int prepTimeMinutes) { this.prepTimeMinutes = prepTimeMinutes; }

    public void setCookTimeMinutes(int cookTimeMinutes) { this.cookTimeMinutes = cookTimeMinutes; }

    public void setEstimatedCalories(double estimatedCalories) { this.estimatedCalories = estimatedCalories; }
}