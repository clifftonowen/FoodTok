package com.example.foodtok.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.foodtok.R;
import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.adapters.FeedAdapter;
import com.example.foodtok.adapters.OnRecipeInteractionListener;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.services.InteractionCallback;
import com.example.foodtok.services.InteractionServiceProvider;
import com.example.foodtok.services.RecipeListCallback;
import com.example.foodtok.services.RecipeServiceProvider;
import com.example.foodtok.services.RecommendationService;
import com.example.foodtok.util.FeedVideoPlayerPool;

import java.util.ArrayList;
import java.util.List;

/** Main feed fragment with vertical ViewPager2 for recipe scrolling and top navigation. */
public class HomeFragment extends Fragment {

  private static final int FEED_PAGE_SIZE = 20;

  private ViewPager2 feedViewPager;
  private FeedAdapter feedAdapter;
  private FeedVideoPlayerPool playerPool;
  private ProgressBar feedLoadingSpinner;
  private View topNavBar;

  private View navIngredientsAction;
  private View navChatAction;
  private View navBackAction;
  private boolean isKeyboardVisible;
  private int currentHorizontalPage = 1;
  private final RecommendationService recommendationService =
      new RecommendationService();

  /** Cached recipes so the feed survives view destruction (e.g. back from overlay). */
  private List<Recipe> cachedRecipes;
  /** Last scroll position before the view was destroyed. */
  private int cachedPosition;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_home, container, false);
    setupTopNav(view);
    setupFeedPager(view);

    ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
      isKeyboardVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime());

      if (feedViewPager != null) {
        // Lock the vertical pager while the keyboard is up, and keep it
        // locked whenever the user is on a non-feed horizontal page
        // (ingredients / chat) — otherwise dismissing the chat keyboard
        // would silently re-enable vertical swiping even though the user
        // is still reading the chat response. Read the page authoritatively
        // from the visible horizontal pager so we don't rely on a cached
        // field that can be stale across keyboard transitions.
        int liveHorizontalPage = (feedAdapter != null)
            ? feedAdapter.getCurrentHorizontalPage()
            : currentHorizontalPage;
        if (liveHorizontalPage < 0) liveHorizontalPage = currentHorizontalPage;
        feedViewPager.setUserInputEnabled(
            !isKeyboardVisible && liveHorizontalPage == 1);
      }

      // Force the Top Nav to highlight "Chat" (index 2) when typing
      if (isKeyboardVisible) {
        updateNavStyling(2);
      }

      return windowInsets;
    });

    return view;
  }

  private void setupTopNav(View view) {
    topNavBar = view.findViewById(R.id.topNavBar);
    navIngredientsAction = view.findViewById(R.id.navIngredientsAction);
    navChatAction = view.findViewById(R.id.navChatAction);
    navBackAction = view.findViewById(R.id.navBackAction);

    // Default active tab = For You
    updateNavStyling(1);

    navIngredientsAction.setOnClickListener(v -> {
      if (feedAdapter != null) {
        feedAdapter.navigateCurrentPageTo(0);
      }
    });

    navChatAction.setOnClickListener(v -> {
      if (feedAdapter != null) {
        feedAdapter.navigateCurrentPageTo(2);
      }
    });

    navBackAction.setOnClickListener(v -> {
      if (feedAdapter != null) {
        feedAdapter.navigateCurrentPageTo(1);
      }
    });
  }

  private void updateNavStyling(int activePage) {
    boolean showingFeed = activePage == 1;
    topNavBar.setVisibility(showingFeed ? View.VISIBLE : View.GONE);
    navBackAction.setVisibility(showingFeed ? View.GONE : View.VISIBLE);

    FrameLayout.LayoutParams backParams =
        (FrameLayout.LayoutParams) navBackAction.getLayoutParams();
    backParams.gravity = Gravity.TOP
        | (activePage == 0 ? Gravity.END : Gravity.START);
    navBackAction.setLayoutParams(backParams);

    // Each detail page points back toward the center feed: Recipes is
    // to its left, while Ask AI is to its right.
    navBackAction.setScaleX(activePage == 0 ? -1f : 1f);
  }

  private void setupFeedPager(View view) {
    feedViewPager = view.findViewById(R.id.feedViewPager);
    feedLoadingSpinner = view.findViewById(R.id.feedLoadingSpinner);

    // Restore from cache if available (e.g. returning from an overlay
    // fragment that destroyed our view).
    if (cachedRecipes != null && !cachedRecipes.isEmpty()) {
      initFeedAdapter(new ArrayList<>(cachedRecipes), cachedPosition);
      return;
    }
    loadFeed();
  }

  /**
   * Re-fetches the feed from the server, tearing down the existing
   * player pool and adapter so the user gets a fresh batch of recipes.
   * Mirrors the effect of re-tapping the Home tab on the bottom nav.
   */
  /** Re-fetches the feed from scratch. Called externally on Home tab reselection. */
  public void refreshFeed() {
    cachedRecipes = null;
    cachedPosition = 0;
    if (playerPool != null) {
      playerPool.release();
      playerPool = null;
    }
    feedAdapter = null;
    if (feedViewPager != null) {
      feedViewPager.setAdapter(null);
    }
    loadFeed();
  }

  private void loadFeed() {
    feedLoadingSpinner.setVisibility(View.VISIBLE);

    RecipeServiceProvider.getRecipeService().getFeedRecipes(
        0, FEED_PAGE_SIZE, new RecipeListCallback() {
          @Override
          public void onSuccess(List<Recipe> recipes) {
            if (getActivity() == null) {
              return;
            }
            getActivity().runOnUiThread(() -> {
              feedLoadingSpinner.setVisibility(View.GONE);
              List<Recipe> ranked =
                  recommendationService.generateFeed(recipes,
                      AuthManager.getInstance().getCurrentUser());
              cachedRecipes = new ArrayList<>(ranked);
              initFeedAdapter(ranked, 0);
            });
          }

          @Override
          public void onError(String message) {
            if (getActivity() == null) {
              return;
            }
            getActivity().runOnUiThread(() -> {
              feedLoadingSpinner.setVisibility(View.GONE);
              showToast("Failed to load feed: " + message);
            });
          }
        });
  }

  private void initFeedAdapter(List<Recipe> recipes, int startPosition) {
    playerPool = new FeedVideoPlayerPool(requireContext());
    playerPool.setRecipes(recipes);

    feedAdapter = new FeedAdapter(recipes, new OnRecipeInteractionListener() {
      @Override
      public void onLikeClicked(Recipe recipe) {
        InteractionServiceProvider.getInteractionService()
            .likeRecipe(recipe.getId(), new InteractionCallback() {
              @Override
              public void onSuccess() {
                // UI already updated optimistically in the click handler.
              }

              @Override
              public void onError(String message) {
                if ("Please log in first".equals(message)) {
                  Intent intent = new Intent(getActivity(), LoginActivity.class);
                  startActivity(intent);
                } else {
                  showToast(message);
                }
              }
            });
      }

      @Override
      public void onCommentClicked(Recipe recipe) {
        CommentsFragment.newInstance(recipe.getId())
            .show(getChildFragmentManager(), "comments");
      }

      @Override
      public void onSaveClicked(Recipe recipe) {
        InteractionServiceProvider.getInteractionService()
            .saveRecipe(recipe.getId(), new InteractionCallback() {
              @Override
              public void onSuccess() {
                // UI already updated optimistically in the click handler.
              }

              @Override
              public void onError(String message) {
                if ("Please log in first".equals(message)) {
                  Intent intent = new Intent(getActivity(), LoginActivity.class);
                  startActivity(intent);
                } else {
                  showToast(message);
                }
              }
            });
      }

      @Override
      public void onNotInterestedClicked(Recipe recipe) {
        InteractionServiceProvider.getInteractionService()
            .markNotInterested(recipe.getId(), new InteractionCallback() {
              @Override
              public void onSuccess() {
                // UI already updated optimistically in the click handler.
              }

              @Override
              public void onError(String message) {
                if ("Please log in first".equals(message)) {
                  Intent intent = new Intent(getActivity(), LoginActivity.class);
                  startActivity(intent);
                } else {
                  showToast(message);
                }
              }
            });
      }
    }, playerPool);

    feedAdapter.setParentVerticalPager(feedViewPager);

    // Update nav style when horizontal page changes inside current recipe card.
    // Also pause video when leaving the center page, resume when returning.
    feedAdapter.setOnHorizontalPageChangedListener((adapterPosition, horizontalPage) -> {
      if (adapterPosition == feedViewPager.getCurrentItem()) {
        currentHorizontalPage = horizontalPage;
        updateNavStyling(horizontalPage);
        if (horizontalPage == 1) {
          playerPool.resumeCurrent();
        } else {
          playerPool.pauseCurrent();
        }
      }
    });

    // Drive player pool on vertical swipe + reset top nav to "For You".
    feedViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
      @Override
      public void onPageSelected(int position) {
        if (!isKeyboardVisible) {
          updateNavStyling(1);
          currentHorizontalPage = 1;
        }
        if (playerPool != null) {
          playerPool.setCurrentPosition(position);
        }
      }
    });

    // Prime the pool BEFORE setting the adapter so the first bind
    // finds a ready player to attach. Otherwise the first video stays
    // blank until the user scrolls away and back.
    playerPool.setCurrentPosition(startPosition);

    feedViewPager.setAdapter(feedAdapter);
    if (startPosition > 0) {
      feedViewPager.setCurrentItem(startPosition, false);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (playerPool != null) {
      playerPool.pauseCurrent();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (playerPool != null) {
      playerPool.resumeCurrent();
    }
  }

  @Override
  public void onDestroyView() {
    if (feedViewPager != null) {
      cachedPosition = feedViewPager.getCurrentItem();
    }
    super.onDestroyView();
    if (playerPool != null) {
      playerPool.release();
      playerPool = null;
    }
  }

  private void showToast(String message) {
    if (getContext() != null) {
      Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
  }

}
