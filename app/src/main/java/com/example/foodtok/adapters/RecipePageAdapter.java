package com.example.foodtok.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodtok.R;
import com.example.foodtok.models.ChatMessage;
import com.example.foodtok.models.Ingredient;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.RecipeEnrichment;
import com.example.foodtok.services.ChatCallback;
import com.example.foodtok.services.ChatServiceProvider;
import com.example.foodtok.services.EnrichmentCallback;
import com.example.foodtok.services.EnrichmentServiceProvider;
import com.example.foodtok.services.InteractionServiceProvider;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Adapter for the inner horizontal ViewPager2 inside each feed item.
 * Manages 3 pages per recipe:
 *   Page 0 — Ingredients list
 *   Page 1 — Video overlay (center, default page)
 *   Page 2 — AI chatbot
 */
public class RecipePageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int PAGE_INGREDIENTS = 0;
    private static final int PAGE_VIDEO = 1;
    private static final int PAGE_CHAT = 2;
    private static final int PAGE_COUNT = 3;

    private final Recipe recipe;
    private final OnRecipeInteractionListener listener;

    private long lastSendTime = 0;
    private static final long SEND_COOLDOWN_MS = 1500; // 1.5 seconds between sends

    public RecipePageAdapter(Recipe recipe, OnRecipeInteractionListener listener) {
        this.recipe = recipe;
        this.listener = listener;
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
        } else {
            holder.recipeDetailAuthor.setText("");
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
            holder.caloriesText.setText("Calories: ~" + (int) cal + " kcal");
        } else {
            holder.nutrientsChip.setText("Nutrients: — kcal/serving");
            holder.caloriesText.setText("Calories: —");
        }

        // Personalized allergen banner — driven by the user's blacklist, not
        // by a per-ingredient flag. If the user has no blacklist (or isn't
        // signed in), this banner stays hidden and we fall back to the
        // Gemini-generated "common allergen" banner once enrichment runs.
        Set<String> userBlacklist = getUserBlacklist();
        List<String> personalMatches = recipe.findBlacklistedIngredients(userBlacklist);
        if (!personalMatches.isEmpty()) {
            StringBuilder pretty = new StringBuilder();
            for (int i = 0; i < personalMatches.size(); i++) {
                String name = personalMatches.get(i);
                pretty.append(name.substring(0, 1).toUpperCase()).append(name.substring(1));
                if (i < personalMatches.size() - 1) pretty.append(", ");
            }
            holder.allergenBanner.setText("⚠ Contains ingredients from your allergen list: " + pretty + ".");
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
                sb.append("\u2610  ")
                        .append(name.substring(0, 1).toUpperCase())
                        .append(name.substring(1));
                if (i < ingredients.size() - 1) {
                    sb.append("\n");
                }
            }
            holder.ingredientsList.setText(sb.toString());
        } else {
            holder.ingredientsList.setText("");
        }

        // Instructions placeholder — populated by enrichment when user taps "Generate"
        holder.instructionsHeader.setText("INSTRUCTIONS");
        holder.instructionsList.setText("Tap \"Generate AI Insights\" to analyse this recipe.");

        // Check if enrichment is already cached (free — no API call)
        RecipeEnrichment cached = EnrichmentServiceProvider.getEnrichmentService()
                .getCachedEnrichment(recipe.getId());
        if (cached != null) {
            applyEnrichment(holder, cached, personalMatches.isEmpty());
            holder.generateAiButton.setVisibility(View.GONE);
        } else {
            holder.generateAiButton.setVisibility(View.VISIBLE);
        }

        // On-demand enrichment — only fires when the user explicitly requests it
        holder.generateAiButton.setOnClickListener(v -> {
            holder.generateAiButton.setEnabled(false);
            holder.generateAiButton.setText("Analysing...");
            EnrichmentServiceProvider.getEnrichmentService().enrichRecipe(recipe,
                    new EnrichmentCallback() {
                        @Override
                        public void onEnriched(RecipeEnrichment enrichment) {
                            holder.generateAiButton.post(() -> {
                                applyEnrichment(holder, enrichment, personalMatches.isEmpty());
                                holder.generateAiButton.setVisibility(View.GONE);
                                // Refresh the Video page to show the new allergen warning
                                notifyItemChanged(PAGE_VIDEO);
                            });
                        }

                        @Override
                        public void onError(String message) {
                            holder.generateAiButton.post(() -> {
                                holder.generateAiButton.setEnabled(true);
                                holder.generateAiButton.setText("✦ Generate AI Insights");
                                holder.instructionsList.setText("Could not generate insights: " + message);
                            });
                        }
                    });
        });
    }

    /**
     * Returns the current user's allergen blacklist as a lowercase set.
     * TODO: wire to UserServiceProvider.getCurrentUser().getBlacklistedIngredients()
     * once the user profile flow is in place. Returning an empty set is safe —
     * it just means no personalized warning, and Gemini enrichment fills in
     * the general common-allergen banner instead.
     */
    private Set<String> getUserBlacklist() {
        return Collections.emptySet(); //TODO: Connect with BackEnd
    }

    private void applyEnrichment(IngredientsViewHolder holder,
                                 RecipeEnrichment enrichment,
                                 boolean noPersonalMatches) {
        // Only let the AI-detected common allergens populate the banner if
        // the personalized blacklist didn't already claim it — the user's
        // own list always takes precedence.
        if (noPersonalMatches && enrichment.hasAllergenWarnings()) {
            holder.allergenBanner.setText("AI Alert: "
                    + String.join(", ", enrichment.getDetectedAllergens())
                    + " commonly trigger allergies.");
            holder.allergenBanner.setVisibility(View.VISIBLE);
        }

        if (enrichment.hasGeneratedInstructions()) {
            List<String> steps = enrichment.getGeneratedInstructions();
            holder.instructionsHeader.setText(
                    "INSTRUCTIONS (" + steps.size() + ") \u2014 AI Generated");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < steps.size(); i++) {
                sb.append(i + 1).append(". ").append(steps.get(i));
                if (i < steps.size() - 1) sb.append("\n\n");
            }
            holder.instructionsList.setText(sb.toString());
        }

        if (enrichment.hasEstimatedCalories()) {
            holder.nutrientsChip.setText("Nutrients: ~"
                    + (int) enrichment.getEstimatedCalories()
                    + " kcal/serving \u2014 AI");
        }
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
        } else {
            holder.recipeTagsText.setText("");
        }

        // Reflect current like/save state
        boolean isLiked = InteractionServiceProvider
                .getInteractionService()
                .isRecipeLiked(recipe.getId());

        boolean isSaved = InteractionServiceProvider
                .getInteractionService()
                .isRecipeSaved(recipe.getId());

        if (isLiked) {
            holder.likeButton.setColorFilter(android.graphics.Color.RED);
        } else {
            holder.likeButton.clearColorFilter();
        }

        if (isSaved) {
            holder.saveButton.setColorFilter(android.graphics.Color.YELLOW);
        } else {
            holder.saveButton.clearColorFilter();
        }

        // Allergen warning — hidden by default, shown when AllergenService is wired
        Set<String> userBlacklist = getUserBlacklist();
        List<String> personalMatches = recipe.findBlacklistedIngredients(userBlacklist);
        if (!personalMatches.isEmpty()) {
            StringBuilder pretty = new StringBuilder();
            for (int i = 0; i < personalMatches.size(); i++) {
                String name = personalMatches.get(i);
                pretty.append(name.substring(0, 1).toUpperCase()).append(name.substring(1));
                if (i < personalMatches.size() - 1) pretty.append(", ");
            }
            holder.allergenWarningText.setText("⚠ Contains: " + pretty);
            holder.allergenWarningText.setVisibility(View.VISIBLE);
        } else {
            RecipeEnrichment cached = EnrichmentServiceProvider.getEnrichmentService()
                    .getCachedEnrichment(recipe.getId());
            if (cached != null && cached.hasAllergenWarnings()) {
                holder.allergenWarningText.setText("AI Alert: "
                        + String.join(", ", cached.getDetectedAllergens())
                        + " commonly trigger allergies.");
                holder.allergenWarningText.setVisibility(View.VISIBLE);
            } else {
                holder.allergenWarningText.setVisibility(View.GONE);
            }
        }

        holder.likeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLikeClicked(recipe);
            }
        });

        holder.commentButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCommentClicked(recipe);
            }
        });

        holder.saveButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSaveClicked(recipe);
            }
        });
    }

    // ── Chat page ───────────────────────────────────────────────────────

    private void bindChat(ChatViewHolder holder) {
        // Set up RecyclerView for chat messages
        if (holder.chatRecyclerView.getLayoutManager() == null) {
            LinearLayoutManager lm = new LinearLayoutManager(holder.itemView.getContext());
            lm.setStackFromEnd(true);
            holder.chatRecyclerView.setLayoutManager(lm);
        }

        // Load existing history or create new adapter
        List<ChatMessage> history = ChatServiceProvider.getChatService()
                .getHistory(recipe.getId());
        ChatMessageAdapter adapter = new ChatMessageAdapter(history);
        holder.chatRecyclerView.setAdapter(adapter);

        // Scroll to bottom if there's existing history
        if (!history.isEmpty()) {
            holder.chatRecyclerView.scrollToPosition(history.size() - 1);
        }

        // Wire send button
        holder.chatSendButton.setOnClickListener(v -> {
            String text = holder.chatInput.getText().toString().trim();
            if (text.isEmpty()) return;

            // Throttle check: reject if within cooldown window
            long now = System.currentTimeMillis();
            long elapsed = now - lastSendTime;
            if (elapsed < SEND_COOLDOWN_MS) {
                long remaining = (SEND_COOLDOWN_MS - elapsed) / 1000 + 1;
                Toast.makeText(holder.itemView.getContext(),
                        "Please wait " + remaining + "s before sending again",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            lastSendTime = now;

            // Clear input and disable send button during API call
            holder.chatInput.setText("");
            holder.chatSendButton.setEnabled(false);

            // Add user message to UI immediately
            ChatMessage userMsg = new ChatMessage("user", text);
            adapter.addMessage(userMsg);
            holder.chatRecyclerView.scrollToPosition(adapter.getItemCount() - 1);

            // Send to chat service
            ChatServiceProvider.getChatService().sendMessage(
                    recipe.getId(), recipe, text, new ChatCallback() {
                        @Override
                        public void onResponse(ChatMessage response) {
                            holder.chatRecyclerView.post(() -> {
                                adapter.addMessage(response);
                                holder.chatRecyclerView.scrollToPosition(
                                        adapter.getItemCount() - 1);
                                reEnableAfterCooldown(holder.chatSendButton);
                            });
                        }

                        @Override
                        public void onError(String message) {
                            holder.chatRecyclerView.post(() -> {
                                ChatMessage errorMsg = new ChatMessage("model",
                                        "Sorry, something went wrong: " + message);
                                adapter.addMessage(errorMsg);
                                holder.chatRecyclerView.scrollToPosition(
                                        adapter.getItemCount() - 1);
                                reEnableAfterCooldown(holder.chatSendButton);
                            });
                        }
                    });
        });
    }

    /**
     * Re-enables the send button only after the full cooldown window has elapsed.
     * If the API responds faster than SEND_COOLDOWN_MS, the button stays disabled
     * for the remaining time to prevent rapid-fire sends.
     */
    private void reEnableAfterCooldown(View sendButton) {
        long elapsed = System.currentTimeMillis() - lastSendTime;
        long remaining = Math.max(0, SEND_COOLDOWN_MS - elapsed);
        sendButton.postDelayed(() -> sendButton.setEnabled(true), remaining);
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
        final Button generateAiButton;
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
            generateAiButton = itemView.findViewById(R.id.generateAiButton);
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
        final RecyclerView chatRecyclerView;
        final EditText chatInput;
        final ImageView chatSendButton;
        final TextView chatTitle;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            chatRecyclerView = itemView.findViewById(R.id.chatRecyclerView);
            chatInput = itemView.findViewById(R.id.chatInput);
            chatSendButton = itemView.findViewById(R.id.chatSendButton);
            chatTitle = itemView.findViewById(R.id.chatTitle);
        }
    }
}