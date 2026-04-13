package com.example.foodtok.services;

import android.util.Log;

import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.models.User;
import com.example.foodtok.models.dto.CreateInteractionRequest;
import com.example.foodtok.models.dto.CreateSavedRecipeRequest;
import com.example.foodtok.models.dto.InteractionDto;
import com.example.foodtok.models.dto.RecipeDto;
import com.example.foodtok.models.dto.SavedRecipeDto;
import com.example.foodtok.models.dto.UpdateProfileRequest;
import com.example.foodtok.models.dto.UserDto;
import com.example.foodtok.util.ApiClient;
import com.example.foodtok.util.SessionManager;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Real {@link IInteractionService} implementation backed by Supabase PostgREST. */
public class SupabaseInteractionService implements IInteractionService {

  private final SupabaseApi api;

  public SupabaseInteractionService() {
    this.api = ApiClient.getRestClient().create(SupabaseApi.class);
  }

  @Override
  public void likeRecipe(String recipeId, InteractionCallback callback) {
    toggleInteraction(recipeId, "like", callback);
  }

  @Override
  public void saveRecipe(String recipeId, InteractionCallback callback) {
    toggleInteraction(recipeId, "save", callback);
  }

  @Override
  public void addComment(String recipeId, String text,
      InteractionCallback callback) {
    // Comment creation is handled by ICommentService.
    // This method exists for interface compatibility.
    callback.onSuccess();
  }

  @Override
  public void isRecipeLiked(String recipeId, BooleanCallback callback) {
    checkInteractionExists(recipeId, "like", callback);
  }

  @Override
  public void isRecipeSaved(String recipeId, BooleanCallback callback) {
    checkInteractionExists(recipeId, "save", callback);
  }

  @Override
  public void markNotInterested(String recipeId,
      InteractionCallback callback) {
    toggleInteraction(recipeId, "not_interested", callback);
  }

  @Override
  public void isRecipeNotInterested(String recipeId,
      BooleanCallback callback) {
    checkInteractionExists(recipeId, "not_interested", callback);
  }

  /**
   * Toggles an interaction: deletes if it exists, creates if it doesn't.
   * Mirrors the toggle behavior of {@link MockInteractionService}.
   */
  private void toggleInteraction(String recipeId, String type,
      InteractionCallback callback) {
    String userId = SessionManager.getInstance().getUserId();
    if (userId == null) {
      callback.onError("Please log in first");
      return;
    }

    String userFilter = "eq." + userId;
    String recipeFilter = "eq." + recipeId;
    String typeFilter = "eq." + type;

    // First check if the interaction already exists
    api.getInteractions(userFilter, recipeFilter, typeFilter)
        .enqueue(new Callback<List<InteractionDto>>() {
          @Override
          public void onResponse(Call<List<InteractionDto>> call,
              Response<List<InteractionDto>> response) {
            if (response.isSuccessful() && response.body() != null
                && !response.body().isEmpty()) {
              // Exists — delete it (un-like / un-save)
              deleteInteraction(userFilter, recipeFilter, typeFilter,
                  callback);
            } else {
              // Doesn't exist — create it
              createInteraction(userId, recipeId, type, callback);
            }
          }

          @Override
          public void onFailure(Call<List<InteractionDto>> call,
              Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  private void createInteraction(String userId, String recipeId,
      String type, InteractionCallback callback) {
    CreateInteractionRequest request =
        new CreateInteractionRequest(userId, recipeId, type);

    api.createInteraction(request)
        .enqueue(new Callback<List<InteractionDto>>() {
          @Override
          public void onResponse(Call<List<InteractionDto>> call,
              Response<List<InteractionDto>> response) {
            if (response.isSuccessful()) {
              if ("save".equals(type)) {
                syncSavedRecipeInsert(userId, recipeId);
                updateInterestProfile(recipeId,
                    RecommendationService.SAVE_POINTS);
              } else if ("like".equals(type)) {
                updateInterestProfile(recipeId,
                    RecommendationService.LIKE_POINTS);
              } else if ("not_interested".equals(type)) {
                updateInterestProfile(recipeId,
                    RecommendationService.NOT_INTERESTED_POINTS);
              }
              callback.onSuccess();
            } else {
              callback.onError("Failed to save interaction: "
                  + response.code());
            }
          }

          @Override
          public void onFailure(Call<List<InteractionDto>> call,
              Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  private void deleteInteraction(String userFilter, String recipeFilter,
      String typeFilter, InteractionCallback callback) {
    api.deleteInteraction(userFilter, recipeFilter, typeFilter)
        .enqueue(new Callback<Void>() {
          @Override
          public void onResponse(Call<Void> call,
              Response<Void> response) {
            if (response.isSuccessful()) {
              if ("eq.save".equals(typeFilter)) {
                syncSavedRecipeDelete(userFilter, recipeFilter);
              }
              // Reverse the score change on un-like / un-save / un-not-interested
              String rawRecipeId = recipeFilter.replace("eq.", "");
              if ("eq.like".equals(typeFilter)) {
                updateInterestProfile(rawRecipeId,
                    -RecommendationService.LIKE_POINTS);
              } else if ("eq.save".equals(typeFilter)) {
                updateInterestProfile(rawRecipeId,
                    -RecommendationService.SAVE_POINTS);
              } else if ("eq.not_interested".equals(typeFilter)) {
                updateInterestProfile(rawRecipeId,
                    -RecommendationService.NOT_INTERESTED_POINTS);
              }
              callback.onSuccess();
            } else {
              callback.onError("Failed to remove interaction: "
                  + response.code());
            }
          }

          @Override
          public void onFailure(Call<Void> call, Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  @Override
  public void getLikeCount(String recipeId, IntCallback callback) {
    fetchInteractionCount(recipeId, "like", callback);
  }

  @Override
  public void getSaveCount(String recipeId, IntCallback callback) {
    fetchInteractionCount(recipeId, "save", callback);
  }

  private void fetchInteractionCount(String recipeId, String type,
      IntCallback callback) {
    // Pass null for user_id so Retrofit omits the param — returns all
    // interactions of this type for the recipe across all users.
    api.getInteractions(null, "eq." + recipeId, "eq." + type)
        .enqueue(new Callback<List<InteractionDto>>() {
          @Override
          public void onResponse(Call<List<InteractionDto>> call,
              Response<List<InteractionDto>> response) {
            if (response.isSuccessful() && response.body() != null) {
              callback.onResult(response.body().size());
            } else {
              callback.onError("Failed to fetch count: " + response.code());
            }
          }

          @Override
          public void onFailure(Call<List<InteractionDto>> call, Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  /**
   * Inserts a matching row into {@code saved_recipes} so the profile
   * Saved tab stays in sync with the {@code interactions} table.
   */
  private void syncSavedRecipeInsert(String userId, String recipeId) {
    api.createSavedRecipe(new CreateSavedRecipeRequest(userId, recipeId))
        .enqueue(new Callback<List<SavedRecipeDto>>() {
          @Override
          public void onResponse(
              Call<List<SavedRecipeDto>> call,
              Response<List<SavedRecipeDto>> resp) {
            // Best-effort sync — no action on failure.
          }

          @Override
          public void onFailure(
              Call<List<SavedRecipeDto>> call,
              Throwable t) {
            // Best-effort sync.
          }
        });
  }

  /**
   * Deletes the matching row from {@code saved_recipes} so the profile
   * Saved tab stays in sync with the {@code interactions} table.
   */
  private void syncSavedRecipeDelete(String userFilter,
      String recipeFilter) {
    api.deleteSavedRecipe(userFilter, recipeFilter)
        .enqueue(new Callback<Void>() {
          @Override
          public void onResponse(Call<Void> call,
              Response<Void> resp) {
            // Best-effort sync.
          }

          @Override
          public void onFailure(Call<Void> call, Throwable t) {
            // Best-effort sync.
          }
        });
  }

  private void checkInteractionExists(String recipeId, String type,
      BooleanCallback callback) {
    String userId = SessionManager.getInstance().getUserId();
    if (userId == null) {
      callback.onResult(false);
      return;
    }

    api.getInteractions("eq." + userId, "eq." + recipeId, "eq." + type)
        .enqueue(new Callback<List<InteractionDto>>() {
          @Override
          public void onResponse(Call<List<InteractionDto>> call,
              Response<List<InteractionDto>> response) {
            boolean exists = response.isSuccessful()
                && response.body() != null
                && !response.body().isEmpty();
            callback.onResult(exists);
          }

          @Override
          public void onFailure(Call<List<InteractionDto>> call,
              Throwable t) {
            callback.onResult(false);
          }
        });
  }

  /**
   * Fetches the recipe's tags, updates the in-memory user interest
   * profile, then PATCHes the profile to Supabase. Best-effort —
   * failures are logged but do not interrupt the interaction flow.
   */
  private void updateInterestProfile(String recipeId, int points) {
    User user = AuthManager.getInstance().getCurrentUser();
    if (user == null) {
      return;
    }

    api.getRecipeById("eq." + recipeId, "tags")
        .enqueue(new Callback<List<RecipeDto>>() {
          @Override
          public void onResponse(Call<List<RecipeDto>> call,
              Response<List<RecipeDto>> response) {
            if (!response.isSuccessful()
                || response.body() == null
                || response.body().isEmpty()) {
              return;
            }
            RecipeDto dto = response.body().get(0);
            if (dto.tags == null || dto.tags.length == 0) {
              return;
            }

            // Update local HashMap
            for (String tag : dto.tags) {
              if (tag != null) {
                user.updateInterestScore(
                    tag.toLowerCase(), points);
              }
            }

            Log.d("RecommendationService",
                "Interest updated (" + (points >= 0 ? "+" : "")
                    + points + " to " + dto.tags.length + " tags) → "
                    + user.getInterestProfile());

            // Persist to Supabase
            persistInterestProfile(user);
          }

          @Override
          public void onFailure(Call<List<RecipeDto>> call,
              Throwable t) {
            Log.w("InteractionService",
                "Failed to fetch recipe tags for profile update",
                t);
          }
        });
  }

  /**
   * PATCHes the user's interest profile to the Supabase profiles
   * table. Best-effort — failures are logged silently.
   */
  private void persistInterestProfile(User user) {
    UpdateProfileRequest request = new UpdateProfileRequest();
    request.interestProfile = user.getInterestProfile();

    api.updateProfile("eq." + user.getId(), request)
        .enqueue(new Callback<List<UserDto>>() {
          @Override
          public void onResponse(Call<List<UserDto>> call,
              Response<List<UserDto>> response) {
            if (!response.isSuccessful()) {
              Log.w("InteractionService",
                  "Profile update failed: " + response.code());
            }
          }

          @Override
          public void onFailure(Call<List<UserDto>> call,
              Throwable t) {
            Log.w("InteractionService",
                "Profile update network error", t);
          }
        });
  }
}
