package com.example.foodtok.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

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

/** Self-contained full-screen doomscroll feed for the grid explore tab. */
public class GridFeedFragment extends Fragment {

  private static final int FEED_PAGE_SIZE = 20;

  /**
   * Optional recipes to display instead of fetching the default feed.
   * Set via {@link #setPendingRecipes(List)} before committing the
   * fragment transaction; consumed and cleared on first use.
   */
  private static List<Recipe> pendingRecipes;

  /** Stashes a specific recipe list for the next GridFeedFragment instance. */
  public static void setPendingRecipes(List<Recipe> recipes) {
    pendingRecipes = recipes;
  }

  private ViewPager2 gridFeedViewPager;
  private FeedAdapter feedAdapter;
  private FeedVideoPlayerPool playerPool;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getActivity() instanceof MainActivity) {
      ((MainActivity) getActivity()).setBottomNavVisibility(false);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (getActivity() instanceof MainActivity) {
      ((MainActivity) getActivity()).setBottomNavVisibility(true);
    }
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_grid_feed, container, false);
    gridFeedViewPager = view.findViewById(R.id.gridFeedViewPager);
    ProgressBar spinner = view.findViewById(R.id.gridFeedLoadingSpinner);

    view.findViewById(R.id.btnGridFeedBack).setOnClickListener(v ->
        requireActivity().getSupportFragmentManager().popBackStack());

    if (pendingRecipes != null) {
      List<Recipe> recipes = pendingRecipes;
      pendingRecipes = null;
      spinner.setVisibility(View.GONE);
      initFeedAdapter(recipes);
      return view;
    }

    RecipeServiceProvider.getRecipeService().getFeedRecipes(
        0, FEED_PAGE_SIZE, new RecipeListCallback() {
          @Override
          public void onSuccess(List<Recipe> recipes) {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
              spinner.setVisibility(View.GONE);
              initFeedAdapter(recipes);
            });
          }

          @Override
          public void onError(String message) {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
              spinner.setVisibility(View.GONE);
              showToast("Failed to load: " + message);
            });
          }
        });

    return view;
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
                  startActivity(new Intent(getActivity(), LoginActivity.class));
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
                  startActivity(new Intent(getActivity(), LoginActivity.class));
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
                  startActivity(new Intent(getActivity(), LoginActivity.class));
                } else {
                  showToast(message);
                }
              }
            });
      }
    }, playerPool);

    feedAdapter.setParentVerticalPager(gridFeedViewPager);

    feedAdapter.setOnHorizontalPageChangedListener((adapterPosition, horizontalPage) -> {
      if (adapterPosition == gridFeedViewPager.getCurrentItem()) {
        if (horizontalPage == 1) {
          playerPool.resumeCurrent();
        } else {
          playerPool.pauseCurrent();
        }
      }
    });

    gridFeedViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
      @Override
      public void onPageSelected(int position) {
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

    gridFeedViewPager.setAdapter(feedAdapter);
    if (startPos > 0) {
      gridFeedViewPager.setCurrentItem(startPos, false);
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
