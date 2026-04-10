package com.example.foodtok.services;

import android.content.Context;
import android.net.Uri;

import java.util.Set;

/** Interface contract for fetching and creating recipes. */
public interface IRecipeService {

  /**
   * Fetches a page of recipes for the feed.
   *
   * @param page     zero-based page index
   * @param pageSize number of recipes per page
   * @param callback async result callback
   */
  void getFeedRecipes(int page, int pageSize, RecipeListCallback callback);

  /**
   * Fetches a single recipe by its ID.
   *
   * @param recipeId the recipe UUID
   * @param callback async result callback
   */
  void getRecipeById(String recipeId, RecipeCallback callback);

  /**
   * Uploads a video to storage and creates a recipe row in the database.
   *
   * @param context  Android context for reading the video Uri
   * @param videoUri local Uri of the video file
   * @param title    recipe title (required)
   * @param description recipe description / instructions
   * @param tags     array of tag strings
   * @param prepTimeMinutes prep time in minutes
   * @param cookTimeMinutes cook time in minutes
   * @param estimatedCalories estimated calorie count
   * @param callback async result callback returning the created Recipe
   */
  void uploadRecipe(Context context, Uri videoUri, String title,
      String description, String[] tags, int prepTimeMinutes,
      int cookTimeMinutes, double estimatedCalories,
      RecipeCallback callback);

  /**
   * Searches recipes by a set of selected tokens (ingredient names OR
   * recipe tags), ranked by the number of distinct tokens each recipe
   * matches. Uses logical OR — a recipe is returned if it matches at
   * least one token, and recipes matching more tokens are ranked first.
   *
   * @param searchTokens lowercase tag or ingredient names to match
   * @param callback     async result callback returning ranked recipes
   */
  void searchByIngredients(Set<String> searchTokens,
      RecipeListCallback callback);
}
