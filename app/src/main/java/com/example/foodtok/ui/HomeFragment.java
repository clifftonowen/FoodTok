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

import java.util.List;

/** Main feed fragment with vertical ViewPager2 for recipe scrolling and top navigation. */
public class HomeFragment extends Fragment {

  private static final float ALPHA_ACTIVE = 1.0f;
  private static final float ALPHA_INACTIVE = 0.45f;
  private static final int FEED_PAGE_SIZE = 20;

  private ViewPager2 feedViewPager;
  private FeedAdapter feedAdapter;
  private ProgressBar feedLoadingSpinner;

  private TextView navIngredients;
  private TextView navForYou;
  private TextView navChat;
  private boolean isKeyboardVisible;

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
      if (feedAdapter != null) {
        feedAdapter.navigateCurrentPageTo(1);
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
        showToast("Comment clicked for " + recipe.getTitle());
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
    });

    feedAdapter.setParentVerticalPager(feedViewPager);

    // Update nav style when horizontal page changes inside current recipe card
    feedAdapter.setOnHorizontalPageChangedListener((adapterPosition, horizontalPage) -> {
      if (adapterPosition == feedViewPager.getCurrentItem()) {
        updateNavStyling(horizontalPage);
      }
    });

    // Reset top nav to "For You" when user swipes to a different recipe
    feedViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
      @Override
      public void onPageSelected(int position) {
        if (!isKeyboardVisible) {
          updateNavStyling(1);
        }
      }
    });

    feedViewPager.setAdapter(feedAdapter);
  }

  private void showToast(String message) {
    if (getContext() != null) {
      Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
  }

}