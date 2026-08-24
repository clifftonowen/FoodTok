package com.example.foodtok.adapters;

import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodtok.R;
import com.example.foodtok.models.dto.RecipeDto;
import com.example.foodtok.util.VideoThumbnailLoader;
import com.example.foodtok.util.PreviewMode;
import androidx.core.content.ContextCompat;

import java.util.List;

public class ProfileRecipeAdapter extends RecyclerView.Adapter<ProfileRecipeAdapter.ViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(int position);
    }

    /** Long-click callback — used for My Recipes tab (delete/download bottom sheet). */
    public interface OnRecipeLongClickListener {
        void onRecipeLongClick(int position);
    }

    /** Overlay interaction callbacks — used for Saved tab. */
    public interface OnRecipeInteractionListener {
        void onLike(RecipeDto recipe);
        void onSave(RecipeDto recipe);
        void onNotInterested(RecipeDto recipe);
    }

    private List<RecipeDto> recipes;
    private OnRecipeClickListener clickListener;
    private OnRecipeLongClickListener longClickListener;
    private OnRecipeInteractionListener interactionListener;

    /** Whether overlay interactions are enabled (Saved tab) vs. long-press bottom sheet (My Recipes). */
    private boolean overlayEnabled = false;

    /** Position of the currently expanded overlay item, or NO_POSITION if none. */
    private int activePosition = RecyclerView.NO_POSITION;

    private int lastAnimatedPosition = -1;

    public ProfileRecipeAdapter(List<RecipeDto> recipes) {
        this.recipes = recipes;
    }

    public void setOnRecipeClickListener(OnRecipeClickListener listener) {
        this.clickListener = listener;
    }

    // Setter for long-click listener
    /** Long-click listener — only used when overlay is disabled (My Recipes tab). */
    public void setOnRecipeLongClickListener(OnRecipeLongClickListener listener) {
        this.longClickListener = listener;
    }

    /** Interaction listener for overlay buttons (Saved tab). */
    public void setOnRecipeInteractionListener(OnRecipeInteractionListener listener) {
        this.interactionListener = listener;
    }

    /**
     * Enable or disable the zoom-overlay mode.
     * When true (Saved tab): long-press shows interaction overlay.
     * When false (My Recipes tab): long-press delegates to longClickListener (bottom sheet).
     */
    public void setOverlayEnabled(boolean enabled) {
        this.overlayEnabled = enabled;
        clearActive();
    }

    /** Collapse any active overlay. Call on scroll or tab switch. */
    public void clearActive() {
        if (activePosition != RecyclerView.NO_POSITION) {
            int old = activePosition;
            activePosition = RecyclerView.NO_POSITION;
            notifyItemChanged(old);
        }
    }

    public void updateData(List<RecipeDto> newRecipes) {
        this.recipes = newRecipes;
        lastAnimatedPosition = -1;
        clearActive();
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
        boolean isActive = overlayEnabled && (position == activePosition);

        // Thumbnail
        if (PreviewMode.isEnabled()) {
            int[] colors = {R.color.foodtok_green_dark, R.color.foodtok_orange_dark,
                    R.color.foodtok_red, R.color.foodtok_green, R.color.foodtok_orange};
            holder.ivRecipeThumb.setBackgroundColor(ContextCompat.getColor(
                    holder.itemView.getContext(), colors[position % colors.length]));
            holder.ivRecipeThumb.setImageResource(R.drawable.ic_burger_foodtok);
            holder.ivRecipeThumb.setPadding(32, 32, 32, 32);
        } else if (!TextUtils.isEmpty(recipe.thumbnailUrl)) {
            Glide.with(holder.itemView.getContext())
                    .load(recipe.thumbnailUrl)
                    .centerCrop()
                    .into(holder.ivRecipeThumb);
        } else {
            VideoThumbnailLoader.load(recipe.videoUrl, holder.ivRecipeThumb);
        }

        // Overlay visibility + scale animation
        if (holder.overlayInteractions != null) {
            holder.overlayInteractions.setVisibility(isActive ? View.VISIBLE : View.GONE);
        }
        holder.itemView.animate()
                .scaleX(isActive ? 1.08f : 1.0f)
                .scaleY(isActive ? 1.08f : 1.0f)
                .setDuration(150)
                .start();

        // Normal tap: open recipe feed (or dismiss overlay if active)
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            if (overlayEnabled && pos == activePosition) {
                clearActive();
            } else if (clickListener != null) {
                clickListener.onRecipeClick(pos);
            }
        });

        // Long-press
        holder.itemView.setOnLongClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return true;

            if (overlayEnabled) {
                // Saved tab: show zoom + overlay
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                int old = activePosition;
                activePosition = pos;
                if (old != RecyclerView.NO_POSITION && old != pos) {
                    notifyItemChanged(old);
                }
                notifyItemChanged(pos);
            } else {
                // My Recipes tab: delegate to fragment (bottom sheet)
                if (longClickListener != null) {
                    longClickListener.onRecipeLongClick(pos);
                }
            }
            return true;
        });

        // Wire overlay buttons (only meaningful when overlay is enabled)
        if (holder.btnOverlayLike != null) {
            holder.btnOverlayLike.setOnClickListener(v -> {
                if (interactionListener != null) interactionListener.onLike(recipe);
                clearActive();
            });
        }
        if (holder.btnOverlaySave != null) {
            holder.btnOverlaySave.setOnClickListener(v -> {
                if (interactionListener != null) interactionListener.onSave(recipe);
                clearActive();
            });
        }
        if (holder.btnOverlayDislike != null) {
            holder.btnOverlayDislike.setOnClickListener(v -> {
                if (interactionListener != null) interactionListener.onNotInterested(recipe);
                clearActive();
            });
        }

        // Staggered entrance animation (only for items appearing for the first time)
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
        LinearLayout overlayInteractions;
        ImageView btnOverlayLike;
        ImageView btnOverlaySave;
        ImageView btnOverlayDislike;

        ViewHolder(View view) {
            super(view);
            ivRecipeThumb = view.findViewById(R.id.ivRecipeThumb);
            overlayInteractions = view.findViewById(R.id.overlayInteractions);
            btnOverlayLike = view.findViewById(R.id.btnOverlayLike);
            btnOverlaySave = view.findViewById(R.id.btnOverlaySave);
            btnOverlayDislike = view.findViewById(R.id.btnOverlayDislike);
        }
    }
}
