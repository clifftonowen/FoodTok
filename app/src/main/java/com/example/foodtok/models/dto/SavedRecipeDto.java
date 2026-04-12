package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

public class SavedRecipeDto {

    @SerializedName("user_id")
    public String userId;

    @SerializedName("recipe_id")
    public String recipeId;

    @SerializedName("created_at")
    public String createdAt;

    public SavedRecipeDto(String userId, String recipeId, String createdAt){
        this.userId = userId;
        this.recipeId = recipeId;
        this.createdAt = createdAt;
    }


}
