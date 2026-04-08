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

    /**
     * Minimal constructor for incremental construction (e.g., upload form).
     * Caller is expected to populate fields via setters and add* methods
     * before persisting the recipe.
     */
    public Recipe(String id, String videoUrl) {
        this(id, "", videoUrl, new ArrayList<>(), new ArrayList<>());
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
     * Used for personalized allergen warnings — the blacklist is managed on
     * the User profile, not on individual ingredients.
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
     * Returns the subset of this recipe's ingredients that match the user's
     * blacklist. Used to build a personalized "Contains X, Y" warning message.
     */
    public List<String> findBlacklistedIngredients(Set<String> blacklistedIngredients) {
        List<String> matches = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            if (blacklistedIngredients.contains(ingredient.getName().toLowerCase())) {
                matches.add(ingredient.getName());
            }
        }
        return matches;
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

    /**
     * Adds a single tag. Used by the upload form to build tags incrementally.
     * Duplicates and blank tags are silently ignored.
     */
    public void addTag(String tag) {
        if (tag == null) return;
        String trimmed = tag.trim();
        if (trimmed.isEmpty() || tags.contains(trimmed)) return;
        tags.add(trimmed);
    }

    /**
     * Adds a single ingredient. Used by the upload form to build the
     * ingredient list one row at a time.
     */
    public void addIngredient(Ingredient ingredient) {
        if (ingredient == null) return;
        ingredients.add(ingredient);
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
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setPrepTimeMinutes(int prepTimeMinutes) { this.prepTimeMinutes = prepTimeMinutes; }
    public void setCookTimeMinutes(int cookTimeMinutes) { this.cookTimeMinutes = cookTimeMinutes; }
    public void setEstimatedCalories(double estimatedCalories) { this.estimatedCalories = estimatedCalories; }
}