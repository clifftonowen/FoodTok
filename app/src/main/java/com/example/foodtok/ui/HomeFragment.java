package com.example.foodtok.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.foodtok.R;
import com.example.foodtok.adapters.FeedAdapter;
import com.example.foodtok.adapters.OnRecipeInteractionListener;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.services.InteractionCallback;
import com.example.foodtok.services.InteractionServiceProvider;
import com.example.foodtok.services.RecipeListCallback;
import com.example.foodtok.services.RecipeServiceProvider;
import com.example.foodtok.util.FeedVideoPlayerPool;

import java.util.List;

/** Main feed fragment with vertical ViewPager2 for recipe scrolling and top navigation. */
public class HomeFragment extends Fragment {

  private static final float ALPHA_ACTIVE = 1.0f;
  private static final float ALPHA_INACTIVE = 0.45f;
  private static final int FEED_PAGE_SIZE = 20;

  private ViewPager2 feedViewPager;
  private FeedAdapter feedAdapter;
  private FeedVideoPlayerPool playerPool;
  private ProgressBar feedLoadingSpinner;

  private TextView navIngredients;
  private TextView navForYou;
  private TextView navChat;
  private boolean isKeyboardVisible;
  private int currentHorizontalPage = 1;

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
        // Lock the vertical pager
        feedViewPager.setUserInputEnabled(!isKeyboardVisible);
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
    navIngredients = view.findViewById(R.id.navIngredients);
    navForYou = view.findViewById(R.id.navForYou);
    navChat = view.findViewById(R.id.navChat);

    // Default active tab = For You
    updateNavStyling(1);

    navIngredients.setOnClickListener(v -> {
      if (feedAdapter != null) {
        feedAdapter.navigateCurrentPageTo(0);
      }
    });

    navForYou.setOnClickListener(v -> {
      // If the user is on Ingredients/Chat, just slide back to the
      // video page — don't tear down the feed. Only refresh when
      // they're already on For You (mirrors re-tapping the bottom nav).
      boolean alreadyOnForYou = currentHorizontalPage == 1;
      if (feedAdapter != null) {
        feedAdapter.navigateCurrentPageTo(1);
      }
      if (alreadyOnForYou) {
        refreshFeed();
      }
    });

    navChat.setOnClickListener(v -> {
      if (feedAdapter != null) {
        feedAdapter.navigateCurrentPageTo(2);
      }
    });
  }

  private void updateNavStyling(int activePage) {
    TextView[] tabs = {navIngredients, navForYou, navChat};
    for (int i = 0; i < tabs.length; i++) {
      boolean active = (i == activePage);
      tabs[i].setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
      tabs[i].animate()
          .alpha(active ? ALPHA_ACTIVE : ALPHA_INACTIVE)
          .setDuration(150)
          .start();
    }
  }

  private void setupFeedPager(View view) {
    feedViewPager = view.findViewById(R.id.feedViewPager);
    feedLoadingSpinner = view.findViewById(R.id.feedLoadingSpinner);
    loadFeed();
  }

  /**
   * Re-fetches the feed from the server, tearing down the existing
   * player pool and adapter so the user gets a fresh batch of recipes.
   * Mirrors the effect of re-tapping the Home tab on the bottom nav.
   */
  private void refreshFeed() {
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
              initFeedAdapter(recipes);
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

  private void initFeedAdapter(List<Recipe> recipes) {
    playerPool = new FeedVideoPlayerPool(requireContext());
    playerPool.setRecipes(recipes);

    feedAdapter = new FeedAdapter(recipes, new OnRecipeInteractionListener() {
      @Override
      public void onLikeClicked(Recipe recipe) {
        InteractionServiceProvider.getInteractionService()
            .likeRecipe(recipe.getId(), new InteractionCallback() {
              @SuppressLint("NotifyDataSetChanged")
              @Override
              public void onSuccess() {
                feedAdapter.notifyDataSetChanged();
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
              @SuppressLint("NotifyDataSetChanged")
              @Override
              public void onSuccess() {
                feedAdapter.notifyDataSetChanged();
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
              @SuppressLint("NotifyDataSetChanged")
              @Override
              public void onSuccess() {
                feedAdapter.notifyDataSetChanged();
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
    int startPos = getArguments() != null ? getArguments().getInt("startPosition", 0) : 0;
    playerPool.setCurrentPosition(startPos);

    feedViewPager.setAdapter(feedAdapter);
    if (startPos > 0) {
      feedViewPager.setCurrentItem(startPos, false);
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