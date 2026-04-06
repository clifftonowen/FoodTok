package com.example.foodtok.services;

public interface IInteractionService {
    void likeButton(String recipeId, InteractionCallback callback );
    void saveRecipe(String recipeId, InteractionCallback callback);

    void addComment(String recipeId, String text, InteractionCallback callback);


}
