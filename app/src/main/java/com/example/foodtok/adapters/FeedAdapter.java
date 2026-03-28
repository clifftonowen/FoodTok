package com.example.foodtok.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodtok.R;
import com.example.foodtok.models.Recipe;

import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.RecipeViewHolder> {

    private final List<Recipe> recipes;

    public FeedAdapter(List<Recipe> recipes) {
        this.recipes = recipes;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);

        holder.recipeTitleText.setText(recipe.getTitle());

        if (recipe.getTags() != null && !recipe.getTags().isEmpty()) {
            holder.recipeTagsText.setText(String.join("  ", recipe.getTags()));
        }

        // Allergen warning is hidden by default; show when needed in future
        holder.allergenWarningText.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView recipeTitleText;
        TextView recipeTagsText;
        TextView allergenWarningText;
        ImageView likeButton;
        ImageView commentButton;
        ImageView saveButton;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeTitleText = itemView.findViewById(R.id.recipeTitleText);
            recipeTagsText = itemView.findViewById(R.id.recipeTagsText);
            allergenWarningText = itemView.findViewById(R.id.allergenWarningText);
            likeButton = itemView.findViewById(R.id.likeButton);
            commentButton = itemView.findViewById(R.id.commentButton);
            saveButton = itemView.findViewById(R.id.saveButton);
        }
    }
}
