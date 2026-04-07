package com.example.foodtok.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodtok.R;
import com.example.foodtok.models.Ingredient;
import com.example.foodtok.models.Recipe;

import java.util.ArrayList;
import java.util.List;

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
        // Title and author
        holder.recipeDetailTitle.setText(recipe.getTitle());
        String author = recipe.getAuthorName();
        if (author != null && !author.isEmpty()) {
            holder.recipeDetailAuthor.setText("by " + author);
        }

        // Time chips
        int prep = recipe.getPrepTimeMinutes();
        int cook = recipe.getCookTimeMinutes();
        holder.prepTimeChip.setText("Prep: " + prep + "m");
        holder.cookTimeChip.setText("Cook: " + cook + "m");

        // Nutrients chip
        double cal = recipe.getEstimatedCalories();
        if (cal > 0) {
            holder.nutrientsChip.setText("Nutrients: ~" + (int) cal + " kcal/serving");
        } else {
            holder.nutrientsChip.setText("Nutrients: — kcal/serving");
        }

        // Allergen banner — show if any ingredient is flagged
        List<String> allergens = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isAllergen()) {
                String name = ingredient.getName();
                allergens.add(name.substring(0, 1).toUpperCase() + name.substring(1));
            }
        }
        if (!allergens.isEmpty()) {
            holder.allergenBanner.setText("Allergen Alert: Contains " + String.join(", ", allergens) + ".");
            holder.allergenBanner.setVisibility(View.VISIBLE);
        } else {
            holder.allergenBanner.setVisibility(View.GONE);
        }

        // Ingredients list with checkbox styling
        List<Ingredient> ingredients = recipe.getIngredients();
        holder.ingredientsHeader.setText("INGREDIENTS (" + ingredients.size() + ")");
        if (!ingredients.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ingredients.size(); i++) {
                String name = ingredients.get(i).getName();
                sb.append("\u2610  ").append(name.substring(0, 1).toUpperCase()).append(name.substring(1));
                if (i < ingredients.size() - 1) {
                    sb.append("\n");
                }
            }
            holder.ingredientsList.setText(sb.toString());
        }

        // Instructions placeholder — will be populated from backend later
        holder.instructionsHeader.setText("INSTRUCTIONS");
        holder.instructionsList.setText("Instructions will be available when connected to the backend.");
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
        final TextView recipeDetailTitle;
        final TextView recipeDetailAuthor;
        final TextView prepTimeChip;
        final TextView cookTimeChip;
        final TextView nutrientsChip;
        final TextView allergenBanner;
        final TextView ingredientsHeader;
        final TextView ingredientsList;
        final TextView instructionsHeader;
        final TextView instructionsList;
        final TextView caloriesText;

        IngredientsViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeDetailTitle = itemView.findViewById(R.id.recipeDetailTitle);
            recipeDetailAuthor = itemView.findViewById(R.id.recipeDetailAuthor);
            prepTimeChip = itemView.findViewById(R.id.prepTimeChip);
            cookTimeChip = itemView.findViewById(R.id.cookTimeChip);
            nutrientsChip = itemView.findViewById(R.id.nutrientsChip);
            allergenBanner = itemView.findViewById(R.id.allergenBanner);
            ingredientsHeader = itemView.findViewById(R.id.ingredientsHeader);
            ingredientsList = itemView.findViewById(R.id.ingredientsList);
            instructionsHeader = itemView.findViewById(R.id.instructionsHeader);
            instructionsList = itemView.findViewById(R.id.instructionsList);
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