package com.example.foodtok.adapters;

import com.example.foodtok.models.Recipe;

public interface OnRecipeInteractionListener {
    void onLikeClicked(Recipe recipe);
    void onCommentClicked(Recipe recipe);
    void onSaveClicked(Recipe recipe);
}