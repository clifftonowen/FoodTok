package com.example.foodtok.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodtok.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the ingredient autocomplete suggestion dropdown.
 * Displays a simple list of matching ingredient names from the Trie.
 */
public class IngredientSuggestionAdapter
    extends RecyclerView.Adapter<IngredientSuggestionAdapter.VH> {

  /** Callback when the user taps a suggestion. */
  public interface OnIngredientSelectedListener {
    void onIngredientSelected(String ingredientName);
  }

  private final List<String> suggestions = new ArrayList<>();
  private OnIngredientSelectedListener listener;

  public void setListener(OnIngredientSelectedListener listener) {
    this.listener = listener;
  }

  /** Replaces the current suggestion list and refreshes the view. */
  public void setSuggestions(List<String> newSuggestions) {
    suggestions.clear();
    suggestions.addAll(newSuggestions);
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_ingredient_suggestion, parent, false);
    return new VH(view);
  }

  @Override
  public void onBindViewHolder(@NonNull VH holder, int position) {
    String name = suggestions.get(position);
    holder.tvSuggestion.setText(name);
    holder.itemView.setOnClickListener(v -> {
      if (listener != null) {
        listener.onIngredientSelected(name);
      }
    });
  }

  @Override
  public int getItemCount() {
    return suggestions.size();
  }

  static class VH extends RecyclerView.ViewHolder {
    final TextView tvSuggestion;

    VH(@NonNull View itemView) {
      super(itemView);
      tvSuggestion = itemView.findViewById(R.id.tvSuggestion);
    }
  }
}