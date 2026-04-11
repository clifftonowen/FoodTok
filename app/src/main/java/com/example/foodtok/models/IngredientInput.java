package com.example.foodtok.models;

/**
 * Small immutable value object used when submitting a new recipe — pairs
 * an ingredient name with a free-text quantity string (e.g. "200g", "2 tbsp").
 */
public final class IngredientInput {

  private final String name;
  private final String quantity;

  public IngredientInput(String name, String quantity) {
    this.name = name;
    this.quantity = quantity;
  }

  public String getName() {
    return name;
  }

  public String getQuantity() {
    return quantity;
  }
}