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

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedPageViewHolder> {

    public interface OnHorizontalPageChangedListener {
        void onPageChanged(int adapterPosition, int horizontalPage);
    }

    private final List<Recipe> recipes;
    private ViewPager2 parentVerticalPager;
    private final OnRecipeInteractionListener interactionListener; // Listener for Likes/Saves
    private OnHorizontalPageChangedListener horizontalPageListener; // Listener for Paging

    public FeedAdapter(List<Recipe> recipes, OnRecipeInteractionListener interactionListener) {
        this.recipes = recipes;
        this.interactionListener = interactionListener;
    }

    public void setParentVerticalPager(ViewPager2 pager) {
        this.parentVerticalPager = pager;
    }

    public void setOnHorizontalPageChangedListener(OnHorizontalPageChangedListener listener) {
        this.horizontalPageListener = listener;
    }

    public void navigateCurrentPageTo(int page) {
        if (parentVerticalPager == null) return;
        View firstChild = parentVerticalPager.getChildAt(0);
        if (!(firstChild instanceof RecyclerView)) return;

        RecyclerView rv = (RecyclerView) firstChild;
        int currentPos = parentVerticalPager.getCurrentItem();
        FeedPageViewHolder vh = (FeedPageViewHolder) rv.findViewHolderForAdapterPosition(currentPos);
        if (vh != null) {
            vh.horizontalPager.setCurrentItem(page, true);
        }
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

        holder.bind(recipe, position, parentVerticalPager, interactionListener, horizontalPageListener);
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class FeedPageViewHolder extends RecyclerView.ViewHolder {

        final ViewPager2 horizontalPager;
        private ViewPager2.OnPageChangeCallback pageChangeCallback;

        FeedPageViewHolder(@NonNull View itemView) {
            super(itemView);
            horizontalPager = itemView.findViewById(R.id.recipeHorizontalPager);
        }


        void bind(Recipe recipe,
                  int adapterPosition,
                  ViewPager2 parentVerticalPager,
                  OnRecipeInteractionListener interactionListener,
                  OnHorizontalPageChangedListener horizontalListener) {

            // Pass the interactionListener to the inner adapter for clicks
            RecipePageAdapter pageAdapter = new RecipePageAdapter(recipe, interactionListener);
            horizontalPager.setAdapter(pageAdapter);

            horizontalPager.setCurrentItem(1, false);

            if (pageChangeCallback != null) {
                horizontalPager.unregisterOnPageChangeCallback(pageChangeCallback);
            }

            pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int page) {
                    if (parentVerticalPager != null) {
                        parentVerticalPager.setUserInputEnabled(page == 1);
                    }
                    if (horizontalListener != null) {
                        horizontalListener.onPageChanged(adapterPosition, page);
                    }
                }
            };
            horizontalPager.registerOnPageChangeCallback(pageChangeCallback);
        }
    }
}