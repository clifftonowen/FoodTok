package com.example.foodtok.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import android.content.Intent;

import com.example.foodtok.models.dto.CreateFollowRequest;
import com.example.foodtok.models.dto.FollowDto;
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.ui.LoginActivity;
import com.example.foodtok.ui.MainActivity;
import com.example.foodtok.ui.OtherUserProfileFragment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import com.example.foodtok.util.ApiClient;
import com.example.foodtok.util.SessionManager;
import com.example.foodtok.util.PreviewMode;
import com.example.foodtok.util.VideoThumbnailLoader;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.annotation.NonNull;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodtok.R;
import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.models.ChatMessage;
import com.example.foodtok.models.Ingredient;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.RecipeEnrichment;
import com.example.foodtok.models.User;
import com.example.foodtok.services.BooleanCallback;
import com.example.foodtok.services.ChatCallback;
import com.example.foodtok.services.ChatServiceProvider;
import com.example.foodtok.services.CommentServiceProvider;
import com.example.foodtok.services.EnrichmentCallback;
import com.example.foodtok.services.EnrichmentServiceProvider;
import com.example.foodtok.services.IntCallback;
import com.example.foodtok.services.InteractionServiceProvider;
import com.example.foodtok.util.FeedVideoPlayerPool;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
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
  private final FeedVideoPlayerPool playerPool;
  private final int feedPosition;

  private long lastSendTime = 0;
  private static final long SEND_COOLDOWN_MS = 1500; // 1.5 seconds between sends

  public RecipePageAdapter(Recipe recipe,
      OnRecipeInteractionListener listener,
      FeedVideoPlayerPool playerPool,
      int feedPosition) {
    this.recipe = recipe;
    this.listener = listener;
    this.playerPool = playerPool;
    this.feedPosition = feedPosition;
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

  @Override
  public void onViewAttachedToWindow(
      @NonNull RecyclerView.ViewHolder holder) {
    super.onViewAttachedToWindow(holder);
    if (holder instanceof VideoViewHolder && playerPool != null) {
      playerPool.attach(feedPosition,
          ((VideoViewHolder) holder).videoView);
    }
  }

  // ── Ingredients page ────────────────────────────────────────────────

  private void bindIngredients(IngredientsViewHolder holder) {
    bindRecipeHero(holder.recipeHeroImage, holder.itemView);

    // Title and author
    holder.recipeDetailTitle.setText(recipe.getTitle());
    String author = recipe.getAuthorName();
    if (author != null && !author.isEmpty()) {
      holder.recipeDetailAuthor.setText("by " + author);
    } else {
      holder.recipeDetailAuthor.setText("");
    }

    // Unified recipe stats
    int prep = recipe.getPrepTimeMinutes();
    int cook = recipe.getCookTimeMinutes();
    holder.prepTimeChip.setText("PREP\n" + prep + " min");
    holder.cookTimeChip.setText("COOK\n" + cook + " min");

    // Nutrients chip
    double cal = recipe.getEstimatedCalories();
    if (cal > 0) {
      holder.nutrientsChip.setText("~" + (int) cal + "\nKCAL");
      holder.caloriesText.setText("Calories: ~" + (int) cal + " kcal");
    } else {
      holder.nutrientsChip.setText("—\nKCAL");
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
      holder.allergenBanner.setText(
          "⚠ Contains allergens: " + pretty + ".");
      holder.allergenBanner.setVisibility(View.VISIBLE);
    } else {
      holder.allergenBanner.setVisibility(View.GONE);
    }

    // Visual ingredient rows make the list scannable at a glance.
    List<Ingredient> ingredients = recipe.getIngredients();
    holder.ingredientsHeader.setText("INGREDIENTS (" + ingredients.size() + ")");

    holder.ingredientsList.removeAllViews();
    for (Ingredient ingredient : ingredients) {
      String name = ingredient.getName() == null ? "" : ingredient.getName().trim();
      if (name.isEmpty()) {
        continue;
      }
      View row = LayoutInflater.from(holder.itemView.getContext())
          .inflate(R.layout.item_ingredient_row, holder.ingredientsList, false);
      TextView icon = row.findViewById(R.id.ingredientIcon);
      TextView label = row.findViewById(R.id.ingredientName);
      TextView quantity = row.findViewById(R.id.ingredientQuantity);
      CheckBox check = row.findViewById(R.id.ingredientCheck);
      icon.setText(iconForIngredient(name));
      icon.setContentDescription(name + " icon");
      label.setText(name.substring(0, 1).toUpperCase() + name.substring(1));
      String amount = ingredient.getQuantity();
      if (amount == null || amount.isEmpty()) amount = "As needed";
      if (ingredient.isOptional()) amount += " · optional";
      quantity.setText(amount);
      check.setContentDescription("Mark " + name + " as prepared");
      check.setOnCheckedChangeListener((button, checked) -> {
        int strike = Paint.STRIKE_THRU_TEXT_FLAG;
        label.setPaintFlags(checked
            ? label.getPaintFlags() | strike
            : label.getPaintFlags() & ~strike);
        quantity.setPaintFlags(checked
            ? quantity.getPaintFlags() | strike
            : quantity.getPaintFlags() & ~strike);
        row.setAlpha(checked ? 0.52f : 1f);
      });
      row.setOnClickListener(v -> check.setChecked(!check.isChecked()));
      holder.ingredientsList.addView(row);
    }

    // Use cached enrichment when available; otherwise keep useful cook-mode
    // guidance without presenting a second AI entry point.
    holder.instructionsHeader.setText("INSTRUCTIONS");
    holder.instructionsList.setText(buildFallbackInstructions());

    // Check if enrichment is already cached (free — no API call)
    RecipeEnrichment cached = EnrichmentServiceProvider.getEnrichmentService()
        .getCachedEnrichment(recipe.getId());
    if (cached != null) {
      applyEnrichment(holder, cached, personalMatches.isEmpty());
      holder.generateAiButton.setText("✓ Generated");
      holder.generateAiButton.setEnabled(false);
    } else {
      holder.generateAiButton.setText("✦ AI Insights");
      holder.generateAiButton.setEnabled(true);
    }

    holder.generateAiButton.setOnClickListener(v -> {
      holder.generateAiButton.setEnabled(false);
      holder.generateAiButton.setText("Analysing…");
      EnrichmentServiceProvider.getEnrichmentService().enrichRecipe(recipe,
          new EnrichmentCallback() {
            @Override
            public void onEnriched(RecipeEnrichment enrichment) {
              holder.generateAiButton.post(() -> {
                applyEnrichment(holder, enrichment, personalMatches.isEmpty());
                holder.generateAiButton.setText("✓ Generated");
                notifyItemChanged(PAGE_VIDEO);
              });
            }

            @Override
            public void onError(String message) {
              holder.generateAiButton.post(() -> {
                holder.generateAiButton.setEnabled(true);
                holder.generateAiButton.setText("Try again");
                Toast.makeText(holder.itemView.getContext(), message,
                    Toast.LENGTH_SHORT).show();
              });
            }
          });
    });
    holder.startCookingButton.setOnClickListener(v ->
        showCookStep(holder.itemView.getContext(), buildCookingSteps(holder), 0));
  }

  private String buildFallbackInstructions() {
    return "1. Gather and measure every ingredient.\n\n"
        + "2. Prepare the ingredients before heating your pan.\n\n"
        + "3. Cook for about " + recipe.getCookTimeMinutes()
        + " minutes, tasting and adjusting as you go.\n\n"
        + "4. Plate and serve while fresh.";
  }

  private List<String> buildCookingSteps(IngredientsViewHolder holder) {
    String raw = holder.instructionsList.getText().toString().trim();
    List<String> steps = new ArrayList<>();
    for (String block : raw.split("\\n\\s*\\n")) {
      String clean = block.replaceFirst("^\\d+[.)]\\s*", "").trim();
      if (!clean.isEmpty()) steps.add(clean);
    }
    if (steps.isEmpty()) steps.add("Prepare the recipe ingredients.");
    return steps;
  }

  private void showCookStep(Context context, List<String> steps, int index) {
    boolean last = index >= steps.size() - 1;
    new androidx.appcompat.app.AlertDialog.Builder(context)
        .setTitle("Step " + (index + 1) + " of " + steps.size())
        .setMessage(steps.get(index))
        .setNegativeButton("Exit", null)
        .setPositiveButton(last ? "Finish" : "Next", (dialog, which) -> {
          if (!last) showCookStep(context, steps, index + 1);
        })
        .show();
  }

  private void bindRecipeHero(ImageView heroImage, View itemView) {
    heroImage.setPadding(0, 0, 0, 0);
    heroImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

    if (PreviewMode.isEnabled()) {
      int[] colors = {R.color.foodtok_green_dark, R.color.foodtok_orange_dark,
          R.color.foodtok_red, R.color.foodtok_green, R.color.foodtok_orange};
      heroImage.setBackgroundColor(ContextCompat.getColor(
          itemView.getContext(), colors[feedPosition % colors.length]));
      int padding = Math.round(64 * itemView.getResources()
          .getDisplayMetrics().density);
      heroImage.setPadding(padding, padding, padding, padding);
      heroImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
      heroImage.setImageResource(R.drawable.ic_burger_foodtok);
      return;
    }

    String thumbnail = recipe.getThumbnailUrl();
    if (thumbnail != null && !thumbnail.trim().isEmpty()) {
      Glide.with(heroImage)
          .load(thumbnail.trim())
          .centerCrop()
          .into(heroImage);
    } else {
      VideoThumbnailLoader.load(recipe.getVideoUrl(), heroImage);
    }
  }

  private String iconForIngredient(String rawName) {
    String name = rawName.toLowerCase();
    if (name.contains("egg")) return "🥚";
    if (name.contains("avocado")) return "🥑";
    if (name.contains("lemon") || name.contains("lime")) return "🍋";
    if (name.contains("chocolate") || name.contains("cocoa")) return "🍫";
    if (name.contains("butter")) return "🧈";
    if (name.contains("cheese") || name.contains("parmesan")) return "🧀";
    if (name.contains("bread") || name.contains("sourdough")) return "🍞";
    if (name.contains("chicken")) return "🍗";
    if (name.contains("noodle") || name.contains("ramen")) return "🍜";
    if (name.contains("pasta") || name.contains("spaghetti")) return "🍝";
    if (name.contains("tortilla") || name.contains("taco")) return "🌮";
    if (name.contains("chili") || name.contains("chilli")
        || name.contains("gochujang")) return "🌶️";
    if (name.contains("cabbage") || name.contains("spinach")
        || name.contains("lettuce")) return "🥬";
    if (name.contains("tomato")) return "🍅";
    if (name.contains("onion")) return "🧅";
    if (name.contains("garlic")) return "🧄";
    if (name.contains("salt") || name.contains("pepper")) return "🧂";
    if (name.contains("sugar")) return "🍬";
    if (name.contains("flour")) return "🌾";
    if (name.contains("broth") || name.contains("soup")) return "🥣";
    return "🥄";
  }

  private Set<String> getUserBlacklist() {
    User user = AuthManager.getInstance().getCurrentUser();
    if (user == null) {
      return Collections.emptySet();
    }
    List<String> blacklist = user.getBlacklistedIngredients();
    if (blacklist == null || blacklist.isEmpty()) {
      return Collections.emptySet();
    }
    return new HashSet<>(blacklist);
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
      holder.nutrientsChip.setText("~" + (int) enrichment.getEstimatedCalories()
          + "\nKCAL · AI");
    }
  }

  // ── Video page (center) ─────────────────────────────────────────────

  private void bindVideo(VideoViewHolder holder) {
    if (playerPool != null) {
      playerPool.attach(feedPosition, holder.videoView);
    }

    if (PreviewMode.isEnabled()) {
      int[] previewColors = {
          R.color.foodtok_green_dark,
          R.color.foodtok_orange_dark,
          R.color.foodtok_red,
          R.color.foodtok_green,
          R.color.foodtok_orange
      };
      holder.stubArtwork.setBackgroundColor(ContextCompat.getColor(
          holder.itemView.getContext(), previewColors[feedPosition % previewColors.length]));
      holder.stubArtwork.setVisibility(View.VISIBLE);
    } else {
      holder.stubArtwork.setVisibility(View.GONE);
    }

    // Tap the video surface to toggle play/pause. Center overlay reflects state.
    holder.playPauseOverlay.setVisibility(View.GONE);
    holder.videoView.setOnClickListener(v -> {
      androidx.media3.common.Player player = holder.videoView.getPlayer();
      if (player == null) {
        return;
      }
      boolean willPlay = !player.getPlayWhenReady();
      player.setPlayWhenReady(willPlay);
      holder.playPauseOverlay.setVisibility(willPlay ? View.GONE : View.VISIBLE);
    });

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

    bindClickableHashtags(holder.recipeTagsText, recipe.getTags());
    bindAuthorAvatar(holder);
    bindFollowState(holder);
    bindAvatarClick(holder);

    // Reflect current like/save/not-interested state (async — set default, update on callback)
    holder.likeButton.clearColorFilter();
    holder.saveButton.clearColorFilter();
    holder.notInterestedButton.clearColorFilter();

    // Reset local toggle state before refetching.
    holder.isLiked = false;
    holder.isSaved = false;
    holder.isNotInterested = false;
    holder.likeCount = 0;
    holder.saveCount = 0;

    InteractionServiceProvider.getInteractionService()
        .isRecipeLiked(recipe.getId(), new BooleanCallback() {
          @Override
          public void onResult(boolean isLiked) {
            holder.likeButton.post(() -> {
              holder.isLiked = isLiked;
              applyLikeTint(holder);
            });
          }

          @Override
          public void onError(String message) {
            // default (no color) is already set
          }
        });

    InteractionServiceProvider.getInteractionService()
        .isRecipeSaved(recipe.getId(), new BooleanCallback() {
          @Override
          public void onResult(boolean isSaved) {
            holder.saveButton.post(() -> {
              holder.isSaved = isSaved;
              applySaveTint(holder);
            });
          }

          @Override
          public void onError(String message) {
            // default (no color) is already set
          }
        });

    InteractionServiceProvider.getInteractionService()
        .isRecipeNotInterested(recipe.getId(), new BooleanCallback() {
          @Override
          public void onResult(boolean isNotInterested) {
            holder.notInterestedButton.post(() -> {
              holder.isNotInterested = isNotInterested;
              applyNotInterestedTint(holder);
            });
          }

          @Override
          public void onError(String message) {
            // default (no color) is already set
          }
        });

    // Fetch and display like / comment / save counts
    holder.likeCountText.setText("0");
    holder.commentCountText.setText("0");
    holder.saveCountText.setText("0");

    InteractionServiceProvider.getInteractionService()
        .getLikeCount(recipe.getId(), new IntCallback() {
          @Override
          public void onResult(int count) {
            holder.likeCountText.post(() -> {
              holder.likeCount = count;
              holder.likeCountText.setText(formatCount(count));
            });
          }

          @Override
          public void onError(String message) { /* keep 0 */ }
        });

    CommentServiceProvider.getCommentService()
        .getCommentCount(recipe.getId(), new IntCallback() {
          @Override
          public void onResult(int count) {
            holder.commentCountText.post(
                () -> holder.commentCountText.setText(formatCount(count)));
          }

          @Override
          public void onError(String message) { /* keep 0 */ }
        });

    InteractionServiceProvider.getInteractionService()
        .getSaveCount(recipe.getId(), new IntCallback() {
          @Override
          public void onResult(int count) {
            holder.saveCountText.post(() -> {
              holder.saveCount = count;
              holder.saveCountText.setText(formatCount(count));
            });
          }

          @Override
          public void onError(String message) { /* keep 0 */ }
        });

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
      holder.isLiked = !holder.isLiked;
      holder.likeCount = Math.max(0,
          holder.likeCount + (holder.isLiked ? 1 : -1));
      applyLikeTint(holder);
      holder.likeCountText.setText(formatCount(holder.likeCount));
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
      holder.isSaved = !holder.isSaved;
      holder.saveCount = Math.max(0,
          holder.saveCount + (holder.isSaved ? 1 : -1));
      applySaveTint(holder);
      holder.saveCountText.setText(formatCount(holder.saveCount));
      if (listener != null) {
        listener.onSaveClicked(recipe);
      }
    });

    holder.notInterestedButton.setOnClickListener(v -> {
      holder.isNotInterested = !holder.isNotInterested;
      applyNotInterestedTint(holder);
      if (listener != null) {
        listener.onNotInterestedClicked(recipe);
      }
    });
  }

  private static void applyLikeTint(VideoViewHolder holder) {
    if (holder.isLiked) {
      holder.likeButton.setColorFilter(ContextCompat.getColor(
          holder.itemView.getContext(), com.example.foodtok.R.color.action_like_active));
    } else {
      holder.likeButton.clearColorFilter();
    }
  }

  private static void applySaveTint(VideoViewHolder holder) {
    if (holder.isSaved) {
      holder.saveButton.setColorFilter(ContextCompat.getColor(
          holder.itemView.getContext(), com.example.foodtok.R.color.action_save_active));
    } else {
      holder.saveButton.clearColorFilter();
    }
  }

  private static void applyNotInterestedTint(VideoViewHolder holder) {
    if (holder.isNotInterested) {
      holder.notInterestedButton.setColorFilter(ContextCompat.getColor(
          holder.itemView.getContext(),
          com.example.foodtok.R.color.action_not_interested_active));
    } else {
      holder.notInterestedButton.clearColorFilter();
    }
  }

  private static String formatCount(int count) {
    if (count >= 1_000_000) return (count / 1_000_000) + "M";
    if (count >= 1_000) return (count / 1_000) + "K";
    return String.valueOf(count);
  }

  // ── Chat page ───────────────────────────────────────────────────────

  private void bindChat(ChatViewHolder holder) {
    bindRecipeHero(holder.chatHeroImage, holder.itemView);

    // Set up RecyclerView for chat messages
    if (holder.chatRecyclerView.getLayoutManager() == null) {
      LinearLayoutManager lm = new LinearLayoutManager(holder.itemView.getContext());
      // Start short conversations below the title instead of pinning the
      // welcome state to the bottom of an otherwise empty screen.
      lm.setStackFromEnd(false);
      holder.chatRecyclerView.setLayoutManager(lm);
    }

    // Load existing history or create new adapter
    List<ChatMessage> history = ChatServiceProvider.getChatService()
        .getHistory(recipe.getId());
    if (history.isEmpty()) {
      history.add(new ChatMessage("model",
          "Hi! Ask me about substitutions, cooking steps, equipment, or nutrition for "
              + recipe.getTitle() + "."));
    }
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

  /**
   * Loads the recipe author's avatar into the bubble at the top of the
   * action column. When the author has no avatar set (e.g. they kept the
   * signup template), we still load the placeholder drawable through
   * Glide so the bubble looks identical to the loaded state — same
   * circle crop, same sizing — instead of leaving the ImageView empty.
   */
  private void bindAuthorAvatar(VideoViewHolder holder) {
    Context ctx = holder.profileImage.getContext().getApplicationContext();
    String url = recipe.getAuthorAvatarUrl();
    boolean hasUrl = url != null && !url.trim().isEmpty();

    if (hasUrl) {
      Glide.with(ctx)
          .load(url.trim())
          .placeholder(R.drawable.ic_profile_placeholder)
          .error(R.drawable.ic_profile_placeholder)
          .circleCrop()
          .dontAnimate()
          .into(holder.profileImage);
    } else {
      // No avatar on file — render the placeholder through Glide so the
      // same circleCrop transformation applies and the bubble never
      // appears blank for template-avatar users.
      Glide.with(ctx)
          .load(R.drawable.ic_profile_placeholder)
          .circleCrop()
          .dontAnimate()
          .into(holder.profileImage);
    }
  }

  /**
   * Hits the {@code follows} table to see whether the current user
   * already follows this recipe's author. Shows the small "+" button
   * under the avatar only when the relationship doesn't exist (or
   * when no one is logged in — they may want to follow after login).
   * Self-authored recipes never show the plus.
   */
  private void bindFollowState(VideoViewHolder holder) {
    String authorId = recipe.getAuthorId();
    if (authorId == null || authorId.isEmpty()) {
      holder.followPlusButton.setVisibility(View.GONE);
      return;
    }

    String currentUserId = SessionManager.getInstance().getUserId();
    if (currentUserId != null && currentUserId.equals(authorId)) {
      holder.followPlusButton.setVisibility(View.GONE);
      return;
    }

    // Optimistically show the plus while we check — this way a slow or
    // failed request never causes the button to disappear. Only a
    // successful response that *confirms* the follow row exists will
    // hide it.
    holder.followPlusButton.setVisibility(View.VISIBLE);
    holder.followPlusButton.setOnClickListener(
        v -> handleFollowClick(holder, authorId));

    if (PreviewMode.isEnabled()) {
      return;
    }

    if (currentUserId == null) {
      return;
    }

    SupabaseApi api = ApiClient.getSupabaseApi();
    api.checkFollow("eq." + currentUserId, "eq." + authorId)
        .enqueue(new Callback<List<FollowDto>>() {
          @Override
          public void onResponse(@NonNull Call<List<FollowDto>> call,
              @NonNull Response<List<FollowDto>> response) {
            if (!response.isSuccessful() || response.body() == null) {
              return;
            }
            boolean alreadyFollowing = !response.body().isEmpty();
            if (alreadyFollowing) {
              holder.followPlusButton.post(() ->
                  holder.followPlusButton.setVisibility(View.GONE));
            }
          }

          @Override
          public void onFailure(@NonNull Call<List<FollowDto>> call,
              @NonNull Throwable t) {
            // Leave the optimistic "+" in place on transient failures.
          }
        });
  }

  /**
   * Fires a follow on the {@code follows} table for the current user →
   * recipe author. If the user isn't signed in, routes them to login
   * instead. On success, hides the plus badge so it mirrors the state
   * reflected by {@link #bindFollowState}.
   */
  private void handleFollowClick(VideoViewHolder holder, String authorId) {
    Context ctx = holder.followPlusButton.getContext();
    if (PreviewMode.isEnabled()) {
      holder.followPlusButton.setVisibility(View.GONE);
      Toast.makeText(ctx, "Following in preview", Toast.LENGTH_SHORT).show();
      return;
    }
    String currentUserId = SessionManager.getInstance().getUserId();
    if (currentUserId == null || currentUserId.isEmpty()) {
      ctx.startActivity(new Intent(ctx, LoginActivity.class));
      return;
    }

    holder.followPlusButton.setEnabled(false);
    SupabaseApi api = ApiClient.getSupabaseApi();
    api.followUser(new CreateFollowRequest(currentUserId, authorId))
        .enqueue(new Callback<List<FollowDto>>() {
          @Override
          public void onResponse(@NonNull Call<List<FollowDto>> call,
              @NonNull Response<List<FollowDto>> response) {
            holder.followPlusButton.post(() -> {
              if (response.isSuccessful()) {
                holder.followPlusButton.setVisibility(View.GONE);
              } else {
                holder.followPlusButton.setEnabled(true);
                Toast.makeText(ctx, "Could not follow user",
                    Toast.LENGTH_SHORT).show();
              }
            });
          }

          @Override
          public void onFailure(@NonNull Call<List<FollowDto>> call,
              @NonNull Throwable t) {
            holder.followPlusButton.post(() -> {
              holder.followPlusButton.setEnabled(true);
              Toast.makeText(ctx, "Network error — try again",
                  Toast.LENGTH_SHORT).show();
            });
          }
        });
  }

  /**
   * Makes the author avatar clickable. Tapping it opens
   * {@link OtherUserProfileFragment} for that author. If the author is
   * the currently logged-in user, the tap is ignored (they can use the
   * Profile tab instead).
   */
  private void bindAvatarClick(VideoViewHolder holder) {
    String authorId = recipe.getAuthorId();
    if (authorId == null || authorId.isEmpty()) {
      holder.profileImage.setOnClickListener(null);
      return;
    }

    String currentUserId = SessionManager.getInstance().getUserId();
    if (currentUserId != null && currentUserId.equals(authorId)) {
      holder.profileImage.setOnClickListener(null);
      return;
    }

    holder.profileImage.setOnClickListener(v -> {
      Context ctx = v.getContext();
      if (ctx instanceof AppCompatActivity) {
        AppCompatActivity activity = (AppCompatActivity) ctx;
        activity.getSupportFragmentManager()
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.fragmentContainer,
                OtherUserProfileFragment.newInstance(authorId))
            .addToBackStack(null)
            .commit();
        if (activity instanceof MainActivity) {
          ((MainActivity) activity).setBottomNavVisibility(false);
        }
      }
    });
  }

  /**
   * Renders the recipe tag list as a single line of "#tag" tokens where
   * each tag is an independent clickable span. Tapping a tag jumps the
   * user to the Search tab with that tag pre-applied as the query.
   */
  private void bindClickableHashtags(TextView target, List<String> tags) {
    if (tags == null || tags.isEmpty()) {
      target.setText("");
      target.setMovementMethod(null);
      return;
    }

    SpannableStringBuilder sb = new SpannableStringBuilder();
    int accent = ContextCompat.getColor(target.getContext(), android.R.color.white);
    for (int i = 0; i < tags.size(); i++) {
      String raw = tags.get(i);
      if (raw == null || raw.trim().isEmpty()) {
        continue;
      }
      final String tag = raw.trim();
      String token = "#" + tag;
      int start = sb.length();
      sb.append(token);
      int end = sb.length();

      ClickableSpan click = new ClickableSpan() {
        @Override
        public void onClick(@NonNull View widget) {
          Context ctx = widget.getContext();
          while (ctx instanceof android.content.ContextWrapper
              && !(ctx instanceof MainActivity)) {
            ctx = ((android.content.ContextWrapper) ctx).getBaseContext();
          }
          if (ctx instanceof MainActivity) {
            ((MainActivity) ctx).navigateToSearchWithTag(tag);
          }
        }
      };
      sb.setSpan(click, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      sb.setSpan(new ForegroundColorSpan(accent), start, end,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

      if (i < tags.size() - 1) {
        sb.append("  ");
      }
    }

    target.setText(sb);
    target.setMovementMethod(LinkMovementMethod.getInstance());
    target.setHighlightColor(android.graphics.Color.TRANSPARENT);
  }

  // ── ViewHolder inner classes ────────────────────────────────────────

  static class IngredientsViewHolder extends RecyclerView.ViewHolder {
    final ImageView recipeHeroImage;
    final TextView recipeDetailTitle;
    final TextView recipeDetailAuthor;
    final TextView prepTimeChip;
    final TextView cookTimeChip;
    final TextView nutrientsChip;
    final TextView allergenBanner;
    final TextView ingredientsHeader;
    final LinearLayout ingredientsList;
    final TextView instructionsHeader;
    final TextView instructionsList;
    final TextView generateAiButton;
    final Button startCookingButton;
    final TextView caloriesText;

    IngredientsViewHolder(@NonNull View itemView) {
      super(itemView);
      recipeHeroImage = itemView.findViewById(R.id.recipeHeroImage);
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
      startCookingButton = itemView.findViewById(R.id.startCookingButton);
      caloriesText = itemView.findViewById(R.id.caloriesText);
    }
  }

  static class VideoViewHolder extends RecyclerView.ViewHolder {
    final PlayerView videoView;
    final ImageView stubArtwork;
    final TextView usernameText;
    final TextView authorNameText;
    final TextView recipeTitleText;
    final TextView recipeTagsText;
    final TextView allergenWarningText;
    final ImageView profileImage;
    final ImageView followPlusButton;
    final ImageView likeButton;
    final TextView likeCountText;
    final ImageView commentButton;
    final TextView commentCountText;
    final ImageView saveButton;
    final TextView saveCountText;
    final ImageView notInterestedButton;
    final ImageView playPauseOverlay;

    // Local toggle/count state used for optimistic UI updates so a
    // like/save/not-interested tap never needs to notifyDataSetChanged
    // (which would re-bind the video and cause a visible flicker).
    boolean isLiked;
    boolean isSaved;
    boolean isNotInterested;
    int likeCount;
    int saveCount;

    VideoViewHolder(@NonNull View itemView) {
      super(itemView);
      videoView = itemView.findViewById(R.id.recipeVideoView);
      stubArtwork = itemView.findViewById(R.id.stubArtwork);
      usernameText = itemView.findViewById(R.id.usernameText);
      authorNameText = itemView.findViewById(R.id.authorNameText);
      recipeTitleText = itemView.findViewById(R.id.recipeTitleText);
      recipeTagsText = itemView.findViewById(R.id.recipeTagsText);
      allergenWarningText = itemView.findViewById(R.id.allergenWarningText);
      profileImage = itemView.findViewById(R.id.profileImage);
      followPlusButton = itemView.findViewById(R.id.followPlusButton);
      likeButton = itemView.findViewById(R.id.likeButton);
      likeCountText = itemView.findViewById(R.id.likeCountText);
      commentButton = itemView.findViewById(R.id.commentButton);
      commentCountText = itemView.findViewById(R.id.commentCountText);
      saveButton = itemView.findViewById(R.id.saveButton);
      saveCountText = itemView.findViewById(R.id.saveCountText);
      notInterestedButton = itemView.findViewById(R.id.notInterestedButton);
      playPauseOverlay = itemView.findViewById(R.id.playPauseOverlay);
    }
  }

  static class ChatViewHolder extends RecyclerView.ViewHolder {
    final ImageView chatHeroImage;
    final RecyclerView chatRecyclerView;
    final EditText chatInput;
    final ImageView chatSendButton;
    final TextView chatTitle;

    ChatViewHolder(@NonNull View itemView) {
      super(itemView);
      chatHeroImage = itemView.findViewById(R.id.chatHeroImage);
      chatRecyclerView = itemView.findViewById(R.id.chatRecyclerView);
      chatInput = itemView.findViewById(R.id.chatInput);
      chatSendButton = itemView.findViewById(R.id.chatSendButton);
      chatTitle = itemView.findViewById(R.id.chatTitle);
    }
  }
}
