package com.example.foodtok.models;

/** Immutable model representing a single cooking ingredient with its calorie value. */
public class Ingredient {

  private final String name;
  private final double calories;
  private final String quantity;
  private final boolean optional;

  public Ingredient(String name, double calories) {
    this(name, calories, "", false);
  }

  public Ingredient(String name, double calories, String quantity,
      boolean optional) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Ingredient name cannot be empty");
    }
    this.name = name.trim().toLowerCase();
    this.calories = calories;
    this.quantity = quantity == null ? "" : quantity.trim();
    this.optional = optional;
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

  public String getQuantity() {
    return quantity;
  }

  public boolean isOptional() {
    return optional;
  }

  // No setters — ingredients don't change after creation.
  // "Tomato" doesn't become "Potato", calories are fixed per ingredient.

}
