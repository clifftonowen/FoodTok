package com.example.foodtok.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodtok.R;
import com.example.foodtok.models.dto.RecipeDto;
import com.example.foodtok.util.VideoThumbnailLoader;

import java.util.List;

public class ProfileRecipeAdapter extends RecyclerView.Adapter<ProfileRecipeAdapter.ViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(int position);
    }

    // [NEW] Long-click callback — used for delete in My Recipes tab
    public interface OnRecipeLongClickListener {
        void onRecipeLongClick(int position);
    }

    private List<RecipeDto> recipes;
    private OnRecipeClickListener clickListener;
    private OnRecipeLongClickListener longClickListener; // [NEW]
    private int lastAnimatedPosition = -1;

    public ProfileRecipeAdapter(List<RecipeDto> recipes) {
        this.recipes = recipes;
    }

    public void setOnRecipeClickListener(OnRecipeClickListener listener) {
        this.clickListener = listener;
    }

    // [NEW] Setter for long-click listener
    public void setOnRecipeLongClickListener(OnRecipeLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void updateData(List<RecipeDto> newRecipes) {
        this.recipes = newRecipes;
        lastAnimatedPosition = -1;
        notifyDataSetChanged();
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

        if (!TextUtils.isEmpty(recipe.thumbnailUrl)) {
            Glide.with(holder.itemView.getContext())
                    .load(recipe.thumbnailUrl)
                    .centerCrop()
                    .into(holder.ivRecipeThumb);
        } else {
            VideoThumbnailLoader.load(recipe.videoUrl, holder.ivRecipeThumb);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    clickListener.onRecipeClick(adapterPosition);
                }
            }
        });

        // [NEW] Wire long-click; return true to consume so regular click doesn't also fire
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    longClickListener.onRecipeLongClick(pos);
                }
            }
            return true;
        });

        // Staggered entrance: only animate items appearing for the first time
        if (position > lastAnimatedPosition) {
            lastAnimatedPosition = position;
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationY(40f);
            holder.itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(350)
                    .setStartDelay(Math.min(position * 50L, 300L))
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecipeThumb;

        ViewHolder(View view) {
            super(view);
            ivRecipeThumb = view.findViewById(R.id.ivRecipeThumb);
        }
    }
}