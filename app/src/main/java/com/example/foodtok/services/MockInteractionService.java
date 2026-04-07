package com.example.foodtok.services;

import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.services.IInteractionService;
import com.example.foodtok.services.InteractionCallback;

import java.util.HashSet;
import java.util.Set;

public class MockInteractionService implements IInteractionService {

    private final Set<String> likedRecipeIds = new HashSet<>();
    private final Set<String> savedRecipeIds = new HashSet<>();


    @Override
    public void likeRecipe(String recipeId, InteractionCallback callback) {
        if (!AuthManager.getInstance().isLoggedIn()) {
            callback.onError("Please log in first");
            return;
        }

        if(likedRecipeIds.contains(recipeId)){
            likedRecipeIds.remove(recipeId);
        }else{
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
        }else{
            savedRecipeIds.add(recipeId);
        }

        callback.onSuccess();
    }

    @Override
    public void addComment(String recipeId, String text, InteractionCallback callback) {
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
    public boolean isRecipeLiked(String recipeId) {
        return likedRecipeIds.contains(recipeId);
    }

    @Override
    public boolean isRecipeSaved(String recipeId) {
        return savedRecipeIds.contains(recipeId);
    }
}