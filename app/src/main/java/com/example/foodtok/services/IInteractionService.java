package com.example.foodtok.services;

/** Interface contract for user-recipe interaction operations (like, save, comment, not interested). */
public interface IInteractionService {

  void likeRecipe(String recipeId, InteractionCallback callback);

  void saveRecipe(String recipeId, InteractionCallback callback);

  void addComment(String recipeId, String text, InteractionCallback callback);

  void markNotInterested(String recipeId, InteractionCallback callback);

  void isRecipeLiked(String recipeId, BooleanCallback callback);

  void isRecipeSaved(String recipeId, BooleanCallback callback);

  void isRecipeNotInterested(String recipeId, BooleanCallback callback);

  void getLikeCount(String recipeId, IntCallback callback);

  void getSaveCount(String recipeId, IntCallback callback);
}
