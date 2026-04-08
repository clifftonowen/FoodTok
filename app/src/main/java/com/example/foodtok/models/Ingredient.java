package com.example.foodtok.models;

public class Ingredient {

    private final String name;
    private final double calories;

    public Ingredient(String name, double calories) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Ingredient name cannot be empty");
        }
        this.name = name.trim().toLowerCase();
        this.calories = calories;
    }

    // --- Getters (all justified) ---

    // Needed: display in recipe detail, search matching, blacklist lookups
    public String getName() {
        return name;
    }

    // Needed: Recipe.calculateCalories() sums these
    public double getCalories() {
        return calories;
    }

    // No setters — ingredients don't change after creation.
    // "Tomato" doesn't become "Potato", calories are fixed per ingredient.

}