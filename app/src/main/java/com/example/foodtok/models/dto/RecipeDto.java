package com.example.foodtok.models.dto;

import com.example.foodtok.models.Ingredient;
import com.example.foodtok.models.Recipe;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DTO for the Supabase {@code recipes} table with nested joins.
 * PostgREST select: {@code *,recipe_ingredients(quantity,is_optional,
 * ingredients(id,name,calories_per_100g)),users!author_id(username,
 * display_name,avatar_url)}
 */
public class RecipeDto {

  @SerializedName("id")
  public String id;

  @SerializedName("author_id")
  public String authorId;

  @SerializedName("title")
  public String title;

  @SerializedName("description")
  public String description;

  @SerializedName("video_url")
  public String videoUrl;

  @SerializedName("thumbnail_url")
  public String thumbnailUrl;

  @SerializedName("tags")
  public String[] tags;

  @SerializedName("prep_time_minutes")
  public int prepTimeMinutes;

  @SerializedName("cook_time_minutes")
  public int cookTimeMinutes;

  @SerializedName("estimated_calories")
  public double estimatedCalories;

  @SerializedName("created_at")
  public String createdAt;

  /** Nested join: recipe_ingredients with inner ingredients. */
  @SerializedName("recipe_ingredients")
  public List<RecipeIngredientDto> recipeIngredients;

  /** Nested join: author profile row via {@code profiles!author_id}. */
  @SerializedName("profiles")
  public UserDto author;

  /** Converts this DTO to the domain {@link Recipe} model. */
  public Recipe toDomain() {
    List<String> tagList = tags != null
        ? Arrays.asList(tags) : new ArrayList<>();

    List<Ingredient> ingredientList = new ArrayList<>();
    if (recipeIngredients != null) {
      for (RecipeIngredientDto ri : recipeIngredients) {
        if (ri.ingredient != null) {
          ingredientList.add(ri.ingredient.toDomain());
        }
      }
    }

    Recipe recipe = new Recipe(id, title, videoUrl, tagList, ingredientList);
    recipe.setDescription(description);
    recipe.setThumbnailUrl(thumbnailUrl);
    recipe.setAuthorId(authorId);
    recipe.setPrepTimeMinutes(prepTimeMinutes);
    recipe.setCookTimeMinutes(cookTimeMinutes);
    recipe.setEstimatedCalories(estimatedCalories);

    if (author != null) {
      if (author.username != null) {
        recipe.setAuthorName(author.username);
      }
      if (author.avatarUrl != null) {
        recipe.setAuthorAvatarUrl(author.avatarUrl);
      }
    }

    return recipe;
  }
}
