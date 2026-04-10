package com.example.foodtok.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.foodtok.R;
import com.example.foodtok.models.dto.RecipeDto;

import java.util.List;

public class ProfileRecipeAdapter extends RecyclerView.Adapter<ProfileRecipeAdapter.ViewHolder> {

    private List<RecipeDto> recipes;

    public ProfileRecipeAdapter(List<RecipeDto> recipes) {
        this.recipes = recipes;
    }

    public void updateData(List<RecipeDto> newRecipes) {
        this.recipes = newRecipes;
        notifyDataSetChanged(); // like React re-render
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RecipeDto recipe = recipes.get(position);
        // TODO: use Glide to load recipe.getImageUrl() into holder.ivRecipeThumb
    }

    @Override
    public int getItemCount() { return recipes.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecipeThumb;
        ViewHolder(View view) {
            super(view);
            ivRecipeThumb = view.findViewById(R.id.ivRecipeThumb);
        }
    }
}