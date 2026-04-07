package com.example.foodtok.services;

public interface IInteractionService {
    void likeRecipe(String recipeId, InteractionCallback callback );
    void saveRecipe(String recipeId, InteractionCallback callback);

    void addComment(String recipeId, String text, InteractionCallback callback);

    boolean isRecipeLiked(String recipeId);
    boolean isRecipeSaved(String recipeId);


}
