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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * RecyclerView adapter for search results displayed in a grid.
 * Shows recipe thumbnail, title, and ingredient match count.
 */
public class SearchResultAdapter
    extends RecyclerView.Adapter<SearchResultAdapter.VH> {

  private final List<Recipe> recipes = new ArrayList<>();
  private Set<String> searchedTokens;

  /** Replaces the result list and refreshes the view. */
  public void setResults(List<Recipe> newRecipes,
      Set<String> tokens) {
    recipes.clear();
    recipes.addAll(newRecipes);
    this.searchedTokens = tokens;
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent,
      int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_search_result, parent, false);
    return new VH(view);
  }

  @Override
  public void onBindViewHolder(@NonNull VH holder, int position) {
    Recipe recipe = recipes.get(position);
    holder.tvTitle.setText(recipe.getTitle());

    if (searchedTokens != null) {
      int matchCount = recipe.countMatchingTokens(searchedTokens);
      int total = searchedTokens.size();
      holder.tvMatchCount.setText(
          matchCount + "/" + total + " matched");
    }

    // Thumbnail placeholder — Glide can be wired here later
    holder.ivThumbnail.setImageResource(
        R.drawable.ic_profile_placeholder);
  }

  @Override
  public int getItemCount() {
    return recipes.size();
  }

  static class VH extends RecyclerView.ViewHolder {
    final ImageView ivThumbnail;
    final TextView tvTitle;
    final TextView tvMatchCount;

    VH(@NonNull View itemView) {
      super(itemView);
      ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
      tvTitle = itemView.findViewById(R.id.tvTitle);
      tvMatchCount = itemView.findViewById(R.id.tvMatchCount);
    }
  }
}