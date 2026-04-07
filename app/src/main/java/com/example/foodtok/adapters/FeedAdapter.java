package com.example.foodtok.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.foodtok.R;
import com.example.foodtok.models.Recipe;

import java.util.List;

/**
 * Adapter for the outer vertical ViewPager2.
 * Each page is a horizontal ViewPager2 containing 3 sub-pages:
 *   [0] Ingredients  |  [1] Video (center, default)  |  [2] Chat
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedPageViewHolder> {

    private final List<Recipe> recipes;
    private ViewPager2 parentVerticalPager;

    private final OnRecipeInteractionListener listener;

    public FeedAdapter(List<Recipe> recipes, OnRecipeInteractionListener listener) {
        this.recipes = recipes;
        this.listener = listener;
    }

    /**
     * Call this from HomeFragment so we can disable vertical scrolling
     * when the user is on a non-video page.
     */
    public void setParentVerticalPager(ViewPager2 pager) {
        this.parentVerticalPager = pager;
    }

    @NonNull
    @Override
    public FeedPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feed_page, parent, false);
        return new FeedPageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedPageViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.bind(recipe, parentVerticalPager, listener);
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class FeedPageViewHolder extends RecyclerView.ViewHolder {

        private final ViewPager2 horizontalPager;

        FeedPageViewHolder(@NonNull View itemView) {
            super(itemView);
            horizontalPager = itemView.findViewById(R.id.recipeHorizontalPager);
        }

        void bind(Recipe recipe, ViewPager2 parentVerticalPager, OnRecipeInteractionListener listener) {
            RecipePageAdapter pageAdapter = new RecipePageAdapter(recipe, listener);
            horizontalPager.setAdapter(pageAdapter);

            //  Always reset to video page (center) when binding.
            // Prevents recycled views from showing the previous recipe's page.
            horizontalPager.setCurrentItem(1, false);

            // Control vertical scrolling based on which horizontal page is active.
            // Only allow vertical swiping when on the video page (page 1).
            horizontalPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    if (parentVerticalPager != null) {
                        parentVerticalPager.setUserInputEnabled(position == 1);
                    }
                }
            });
        }
    }
}
