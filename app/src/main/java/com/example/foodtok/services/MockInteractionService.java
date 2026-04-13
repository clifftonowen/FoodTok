package com.example.foodtok.services;

import com.example.foodtok.auth.AuthManager;

import java.util.HashSet;
import java.util.Set;

/** In-memory mock implementation of {@link IInteractionService} for testing without a backend. */
public class MockInteractionService implements IInteractionService {

  private final Set<String> likedRecipeIds = new HashSet<>();
  private final Set<String> savedRecipeIds = new HashSet<>();
  private final Set<String> notInterestedRecipeIds = new HashSet<>();

  @Override
  public void likeRecipe(String recipeId, InteractionCallback callback) {
    if (!AuthManager.getInstance().isLoggedIn()) {
      callback.onError("Please log in first");
      return;
    }

    if (likedRecipeIds.contains(recipeId)) {
      likedRecipeIds.remove(recipeId);
    } else {
      likedRecipeIds.add(recipeId);
    }

    callback.onSuccess();
  }

  @Override
  public void saveRecipe(String recipeId, InteractionCallback callback) {
    if (!AuthManager.getInstance().isLoggedIn()) {
      callback.onError("Please log in first");
      return;
    }

    if (savedRecipeIds.contains(recipeId)) {
      savedRecipeIds.remove(recipeId);
    } else {
      savedRecipeIds.add(recipeId);
    }

    callback.onSuccess();
  }

  @Override
  public void addComment(String recipeId, String text,
      InteractionCallback callback) {
    if (!AuthManager.getInstance().isLoggedIn()) {
      callback.onError("Please log in first");
      return;
    }

    if (text == null || text.trim().isEmpty()) {
      callback.onError("Comment cannot be empty");
      return;
    }

    callback.onSuccess();
  }

  @Override
  public void markNotInterested(String recipeId,
      InteractionCallback callback) {
    if (!AuthManager.getInstance().isLoggedIn()) {
      callback.onError("Please log in first");
      return;
    }

    if (notInterestedRecipeIds.contains(recipeId)) {
      notInterestedRecipeIds.remove(recipeId);
    } else {
      notInterestedRecipeIds.add(recipeId);
    }

    callback.onSuccess();
  }

  @Override
  public void isRecipeLiked(String recipeId, BooleanCallback callback) {
    callback.onResult(likedRecipeIds.contains(recipeId));
  }

  @Override
  public void isRecipeSaved(String recipeId, BooleanCallback callback) {
    callback.onResult(savedRecipeIds.contains(recipeId));
  }

  @Override
  public void isRecipeNotInterested(String recipeId,
      BooleanCallback callback) {
    callback.onResult(notInterestedRecipeIds.contains(recipeId));
  }

  @Override
  public void getLikeCount(String recipeId, IntCallback callback) {
    callback.onResult(0);
  }

  @Override
  public void getSaveCount(String recipeId, IntCallback callback) {
    callback.onResult(0);
  }
}
