package com.example.foodtok.services;

/** Interface contract for fetching and posting recipe comments. */
public interface ICommentService {

  /**
   * Fetches all comments for a recipe, newest first.
   *
   * @param recipeId the recipe UUID
   * @param callback async result callback
   */
  void getComments(String recipeId, CommentListCallback callback);

  /**
   * Posts a new comment on a recipe.
   *
   * @param recipeId the recipe UUID
   * @param text     comment content
   * @param callback async result callback with the created comment
   */
  void postComment(String recipeId, String text, CommentCallback callback);

  void getCommentCount(String recipeId, IntCallback callback);
}
