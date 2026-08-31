package com.example.foodtok;

import android.app.Application;

import android.util.Log;

import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.auth.AuthServiceProvider;
import com.example.foodtok.auth.MockAuthService;
import com.example.foodtok.auth.SupabaseAuthService;
import com.example.foodtok.models.User;
import com.example.foodtok.models.dto.UserDto;
import com.example.foodtok.services.CommentServiceProvider;
import com.example.foodtok.services.InteractionServiceProvider;
import com.example.foodtok.services.MockCommentService;
import com.example.foodtok.services.MockInteractionService;
import com.example.foodtok.services.MockRecipeService;
import com.example.foodtok.services.RecipeServiceProvider;
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.services.SupabaseCommentService;
import com.example.foodtok.services.SupabaseInteractionService;
import com.example.foodtok.services.SupabaseRecipeService;
import com.example.foodtok.util.ApiClient;
import com.example.foodtok.util.PreviewMode;
import com.example.foodtok.util.SessionManager;
import com.example.foodtok.util.ThemePreferences;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Custom Application entry point. Initializes session management and service providers. */
public class FoodTokApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    // Apply appearance before any activity is created. A fresh install starts dark.
    ThemePreferences.applySavedTheme(this);

    // 1. Initialize SessionManager (needs Context, so must be first)
    SessionManager.init(this);

    if (PreviewMode.isEnabled()) {
      configurePreviewMode();
      return;
    }

    // 2. Set real auth service (replaces MockAuthService)
    AuthServiceProvider.setAuthService(new SupabaseAuthService());

    // 3. Set real Supabase services (replaces mock implementations)
    RecipeServiceProvider.setRecipeService(new SupabaseRecipeService());
    CommentServiceProvider.setCommentService(new SupabaseCommentService());
    InteractionServiceProvider.setInteractionService(
        new SupabaseInteractionService());

    // 4. Restore login state if user was previously logged in
    restoreSession();
  }

  /** Installs only in-memory services and a demo identity; no API client is created. */
  private void configurePreviewMode() {
    AuthServiceProvider.setAuthService(new MockAuthService());
    RecipeServiceProvider.setRecipeService(new MockRecipeService());
    CommentServiceProvider.setCommentService(new MockCommentService());
    InteractionServiceProvider.setInteractionService(new MockInteractionService());

    User demoUser = new User("preview-user", "yourkitchen", "preview@foodtok.local");
    demoUser.addBlacklistedIngredient("peanuts");
    demoUser.updateInterestScore("quick", 20);
    demoUser.updateInterestScore("asian", 15);
    AuthManager.getInstance().login(demoUser);
    Log.i("FoodTokApp", "Offline preview mode enabled");
  }

  private void restoreSession() {
    SessionManager session = SessionManager.getInstance();

    if (session.isLoggedIn()) {
      String userId = session.getUserId();
      String username = session.getUsername();
      User user = new User(userId, username, "");
      AuthManager.getInstance().login(user);

      // Fetch the full profile (interest_profile, blacklisted
      // ingredients) so the recommendation algorithm has data.
      fetchUserProfile(userId, user);
    }
  }

  /**
   * Loads the user's interest profile and blacklisted ingredients
   * from Supabase into the in-memory {@link User} object. Best-effort
   * — the feed will still load without personalization if this fails.
   */
  private void fetchUserProfile(String userId, User user) {
    SupabaseApi api =
        ApiClient.getRestClient().create(SupabaseApi.class);
    api.getProfiles("eq." + userId,
        "id,interest_profile,blacklisted_ingredients")
        .enqueue(new Callback<List<UserDto>>() {
          @Override
          public void onResponse(Call<List<UserDto>> call,
              Response<List<UserDto>> response) {
            if (response.isSuccessful()
                && response.body() != null
                && !response.body().isEmpty()) {
              UserDto dto = response.body().get(0);
              if (dto.interestProfile != null) {
                for (Map.Entry<String, Integer> entry
                    : dto.interestProfile.entrySet()) {
                  user.updateInterestScore(
                      entry.getKey(), entry.getValue());
                }
              }
              if (dto.blacklistedIngredients != null) {
                for (String ingredient
                    : dto.blacklistedIngredients) {
                  user.addBlacklistedIngredient(ingredient);
                }
              }
              Log.d("FoodTokApp",
                  "Loaded interest profile: "
                      + user.getInterestProfile().size()
                      + " tags");
            }
          }

          @Override
          public void onFailure(Call<List<UserDto>> call,
              Throwable t) {
            Log.w("FoodTokApp",
                "Failed to load user profile", t);
          }
        });
  }
}
