package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

/** POST body for a single row in the {@code recipe_ingredients} join table. */
public class CreateRecipeIngredientRequest {

  @SerializedName("recipe_id")
  public String recipeId;

  @SerializedName("ingredient_id")
  public String ingredientId;

  @SerializedName("quantity")
  public String quantity;

  @SerializedName("is_optional")
  public boolean isOptional;

  public CreateRecipeIngredientRequest(String recipeId, String ingredientId,
      String quantity, boolean isOptional) {
    this.recipeId = recipeId;
    this.ingredientId = ingredientId;
    this.quantity = quantity;
    this.isOptional = isOptional;
  }
}
