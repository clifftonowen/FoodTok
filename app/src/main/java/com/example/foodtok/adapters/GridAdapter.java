package com.example.foodtok.adapters;

import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodtok.R;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.util.VideoThumbnailLoader;
import com.example.foodtok.util.PreviewMode;
import androidx.core.content.ContextCompat;

import java.util.List;

/** Adapter for the 2-column recipe thumbnail grid in GridFragment. */
public class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {

  public interface OnItemClickListener {
    void onItemClick(int position);
  }

  /** Fired when an overlay interaction button is tapped on a grid item. */
  public interface OnGridInteractionListener {
    void onLike(Recipe recipe);
    void onSave(Recipe recipe);
    void onNotInterested(Recipe recipe);
  }

  private final List<Recipe> recipes;
  private final OnItemClickListener listener;
  private OnGridInteractionListener interactionListener;

  /** Position of the currently expanded (overlay-visible) item, or NO_POSITION if none. */
  private int activePosition = RecyclerView.NO_POSITION;

  public GridAdapter(List<Recipe> recipes, OnItemClickListener listener) {
    this.recipes = recipes;
    this.listener = listener;
  }

  public void setOnGridInteractionListener(OnGridInteractionListener listener) {
    this.interactionListener = listener;
  }

  /** Collapses any active overlay. Call on scroll or external dismiss. */
  public void clearActive() {
    if (activePosition != RecyclerView.NO_POSITION) {
      int old = activePosition;
      activePosition = RecyclerView.NO_POSITION;
      notifyItemChanged(old);
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_grid_recipe, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    Recipe recipe = recipes.get(position);
    boolean isActive = (position == activePosition);

    // Thumbnail
    if (PreviewMode.isEnabled()) {
      int[] colors = {R.color.foodtok_green_dark, R.color.foodtok_orange_dark,
          R.color.foodtok_red, R.color.foodtok_green, R.color.foodtok_orange};
      holder.ivRecipeThumb.setBackgroundColor(ContextCompat.getColor(
          holder.itemView.getContext(), colors[position % colors.length]));
      holder.ivRecipeThumb.setImageResource(R.drawable.ic_burger_foodtok);
      holder.ivRecipeThumb.setPadding(42, 42, 42, 42);
    } else if (!TextUtils.isEmpty(recipe.getThumbnailUrl())) {
      Glide.with(holder.ivRecipeThumb)
          .load(recipe.getThumbnailUrl())
          .centerCrop()
          .into(holder.ivRecipeThumb);
    } else {
      Glide.with(holder.ivRecipeThumb).clear(holder.ivRecipeThumb);
      VideoThumbnailLoader.load(recipe.getVideoUrl(), holder.ivRecipeThumb);
    }

    // Overlay visibility + scale (animate for smooth feel)
    holder.overlayInteractions.setVisibility(isActive ? View.VISIBLE : View.GONE);
    holder.itemView.animate()
        .scaleX(isActive ? 1.08f : 1.0f)
        .scaleY(isActive ? 1.08f : 1.0f)
        .setDuration(150)
        .start();

    // Normal tap: open feed (or dismiss overlay if active)
    holder.itemView.setOnClickListener(v -> {
      int pos = holder.getAdapterPosition();
      if (pos == RecyclerView.NO_POSITION) return;
      if (pos == activePosition) {
        // Dismiss overlay on tap
        clearActive();
      } else {
        listener.onItemClick(pos);
      }
    });

    // Long press: show overlay on this item
    holder.itemView.setOnLongClickListener(v -> {
      int pos = holder.getAdapterPosition();
      if (pos == RecyclerView.NO_POSITION) return true;

      v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

      int old = activePosition;
      activePosition = pos;
      if (old != RecyclerView.NO_POSITION && old != pos) {
        notifyItemChanged(old);
      }
      notifyItemChanged(pos);
      return true;
    });

    // Overlay interaction buttons
    holder.btnOverlayLike.setOnClickListener(v -> {
      if (interactionListener != null) interactionListener.onLike(recipe);
      clearActive();
    });
    holder.btnOverlaySave.setOnClickListener(v -> {
      if (interactionListener != null) interactionListener.onSave(recipe);
      clearActive();
    });
    holder.btnOverlayDislike.setOnClickListener(v -> {
      if (interactionListener != null) interactionListener.onNotInterested(recipe);
      clearActive();
    });
  }

  @Override
  public int getItemCount() {
    return recipes.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    final ImageView ivRecipeThumb;
    final LinearLayout overlayInteractions;
    final ImageView btnOverlayLike;
    final ImageView btnOverlaySave;
    final ImageView btnOverlayDislike;

    ViewHolder(@NonNull View itemView) {
      super(itemView);
      ivRecipeThumb = itemView.findViewById(R.id.ivRecipeThumb);
      overlayInteractions = itemView.findViewById(R.id.overlayInteractions);
      btnOverlayLike = itemView.findViewById(R.id.btnOverlayLike);
      btnOverlaySave = itemView.findViewById(R.id.btnOverlaySave);
      btnOverlayDislike = itemView.findViewById(R.id.btnOverlayDislike);
    }
  }
}
