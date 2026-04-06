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

/**
 * Adapter for the inner horizontal ViewPager2 inside each feed item.
 * Manages 3 pages per recipe:
 *   Page 0 — Ingredients list
 *   Page 1 — Video overlay (center, default page)
 *   Page 2 — AI chat stub
 */
public class RecipePageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int PAGE_INGREDIENTS = 0;
    private static final int PAGE_VIDEO = 1;
    private static final int PAGE_CHAT = 2;
    private static final int PAGE_COUNT = 3;

    private final Recipe recipe;

    public RecipePageAdapter(Recipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case PAGE_INGREDIENTS:
                return new IngredientsViewHolder(
                        inflater.inflate(R.layout.fragment_recipe_ingredients, parent, false));
            case PAGE_VIDEO:
                return new VideoViewHolder(
                        inflater.inflate(R.layout.item_recipe, parent, false));
            case PAGE_CHAT:
                return new ChatViewHolder(
                        inflater.inflate(R.layout.fragment_recipe_chat, parent, false));
            default:
                throw new IllegalArgumentException("Invalid page position: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (position) {
            case PAGE_INGREDIENTS:
                bindIngredients((IngredientsViewHolder) holder);
                break;
            case PAGE_VIDEO:
                bindVideo((VideoViewHolder) holder);
                break;
            case PAGE_CHAT:
                bindChat((ChatViewHolder) holder);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }

    // ── Ingredients page ────────────────────────────────────────────────

    private void bindIngredients(IngredientsViewHolder holder) {
        if (recipe.getIngredients() != null && !recipe.getIngredients().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < recipe.getIngredients().size(); i++) {
                sb.append("• ").append(recipe.getIngredients().get(i));
                if (i < recipe.getIngredients().size() - 1) {
                    sb.append("\n");
                }
            }
            holder.ingredientsList.setText(sb.toString());
        }
        // Calories placeholder — will be populated by OpenAI integration later
        holder.caloriesText.setText("Calories: —");
    }

    // ── Video page (center) ─────────────────────────────────────────────

    private void bindVideo(VideoViewHolder holder) {
        holder.recipeTitleText.setText(recipe.getTitle());

        // Display User.name as the username handle
        String authorName = recipe.getAuthorName();
        if (authorName != null && !authorName.isEmpty()) {
            holder.usernameText.setText("@" + authorName);
            holder.usernameText.setVisibility(View.VISIBLE);
            holder.authorNameText.setText(authorName);
        } else {
            holder.usernameText.setVisibility(View.GONE);
            holder.authorNameText.setText("");
        }

        if (recipe.getTags() != null && !recipe.getTags().isEmpty()) {
            holder.recipeTagsText.setText(String.join("  ", recipe.getTags()));
        }

        // Allergen warning — hidden by default, shown when AllergenService is wired
        holder.allergenWarningText.setVisibility(View.GONE);

        // TODO: wire like/comment/save buttons via OnRecipeInteractionListener
    }

    // ── Chat page ───────────────────────────────────────────────────────

    private void bindChat(ChatViewHolder holder) {
        // Stub — will be wired to OpenAI chat in Phase 5
    }

    // ── ViewHolder inner classes ────────────────────────────────────────

    static class IngredientsViewHolder extends RecyclerView.ViewHolder {
        final TextView ingredientsList;
        final TextView caloriesText;

        IngredientsViewHolder(@NonNull View itemView) {
            super(itemView);
            ingredientsList = itemView.findViewById(R.id.ingredientsList);
            caloriesText = itemView.findViewById(R.id.caloriesText);
        }
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        final TextView usernameText;
        final TextView authorNameText;
        final TextView recipeTitleText;
        final TextView recipeTagsText;
        final TextView allergenWarningText;
        final ImageView likeButton;
        final ImageView commentButton;
        final ImageView saveButton;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.usernameText);
            authorNameText = itemView.findViewById(R.id.authorNameText);
            recipeTitleText = itemView.findViewById(R.id.recipeTitleText);
            recipeTagsText = itemView.findViewById(R.id.recipeTagsText);
            allergenWarningText = itemView.findViewById(R.id.allergenWarningText);
            likeButton = itemView.findViewById(R.id.likeButton);
            commentButton = itemView.findViewById(R.id.commentButton);
            saveButton = itemView.findViewById(R.id.saveButton);
        }
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            // TODO: bind chat RecyclerView, input field, send button
        }
    }
}