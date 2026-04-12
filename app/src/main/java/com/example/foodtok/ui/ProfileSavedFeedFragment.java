package com.example.foodtok.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.foodtok.R;
import com.example.foodtok.adapters.FeedAdapter;
import com.example.foodtok.adapters.OnRecipeInteractionListener;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.dto.RecipeDto;
import com.example.foodtok.models.dto.SavedRecipeDto;
import com.example.foodtok.services.InteractionCallback;
import com.example.foodtok.services.InteractionServiceProvider;
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.util.ApiClient;
import com.example.foodtok.util.FeedVideoPlayerPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileSavedFeedFragment extends Fragment {

    private static final String ARG_START_POSITION = "startPosition";
    private static final String ARG_USER_ID = "userId";
    private static final String ARG_FEED_MODE = "feedMode";
    public static final String MODE_MY_RECIPES = "my_recipes";
    public static final String MODE_SAVED = "saved";

    private ViewPager2 viewPager;
    private FeedAdapter feedAdapter;
    private FeedVideoPlayerPool playerPool;
    private final List<Recipe> recipes = new ArrayList<>();

    public static ProfileSavedFeedFragment newInstance(int startPosition, String feedMode, String userId) {
        ProfileSavedFeedFragment fragment = new ProfileSavedFeedFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_START_POSITION, startPosition);
        args.putString(ARG_FEED_MODE, feedMode);
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisibility(false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_feed, container, false);

        viewPager = view.findViewById(R.id.vpProfileFeed);
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        String feedMode = getArguments() != null
                ? getArguments().getString(ARG_FEED_MODE, MODE_SAVED)
                : MODE_SAVED;
        if (MODE_MY_RECIPES.equals(feedMode)) {
            loadMyRecipes();
        } else {
            loadSavedRecipes();
        }
        return view;
    }

    private void initFeedAdapter(List<Recipe> loadedRecipes, int startPosition) {
        if (!isAdded()) {
            return;
        }

        recipes.clear();
        recipes.addAll(loadedRecipes);

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

        feedAdapter.setParentVerticalPager(viewPager);

        feedAdapter.setOnHorizontalPageChangedListener((adapterPosition, horizontalPage) -> {
            if (adapterPosition == viewPager.getCurrentItem()) {
                if (horizontalPage == 1) {
                    playerPool.resumeCurrent();
                } else {
                    playerPool.pauseCurrent();
                }
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (playerPool != null) {
                    playerPool.setCurrentPosition(position);
                }
            }
        });

        viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setAdapter(feedAdapter);

        if (startPosition > 0) {
            viewPager.setCurrentItem(startPosition, false);
        }

        viewPager.post(() -> {
            if (playerPool != null) {
                playerPool.setCurrentPosition(viewPager.getCurrentItem());
            }
        });
    }

    private void loadMyRecipes() {
        if (getArguments() == null) return;

        String userId = getArguments().getString(ARG_USER_ID, "");
        int startPosition = getArguments().getInt(ARG_START_POSITION, 0);

        SupabaseApi api = ApiClient.getSupabaseApi();

        api.getRecipesByAuthor(
                "eq." + userId,
                "id,author_id,title,description,video_url,thumbnail_url,tags,prep_time_minutes,cook_time_minutes,estimated_calories,created_at"
        ).enqueue(new Callback<List<RecipeDto>>() {
            @Override
            public void onResponse(Call<List<RecipeDto>> call, Response<List<RecipeDto>> response) {
                if (!response.isSuccessful() || response.body() == null || !isAdded()) {
                    showToast("Failed to load recipes");
                    return;
                }
                List<Recipe> loaded = new ArrayList<>();
                for (RecipeDto dto : response.body()) {
                    loaded.add(dto.toDomain());
                }
                initFeedAdapter(loaded, startPosition);
            }

            @Override
            public void onFailure(Call<List<RecipeDto>> call, Throwable t) {
                showToast("Failed to load recipes");
            }
        });
    }

    private void loadSavedRecipes() {
        if (getArguments() == null) {
            return;
        }

        String userId = getArguments().getString(ARG_USER_ID, "");
        int startPosition = getArguments().getInt(ARG_START_POSITION, 0);

        SupabaseApi api = ApiClient.getSupabaseApi();

        api.getSavedRecipes(
                "eq." + userId,
                "user_id,recipe_id,created_at",
                "created_at.desc"
        ).enqueue(new Callback<List<SavedRecipeDto>>() {
            @Override
            public void onResponse(Call<List<SavedRecipeDto>> call, Response<List<SavedRecipeDto>> response) {
                if (!response.isSuccessful() || response.body() == null || !isAdded()) {
                    showToast("Failed to load saved recipes");
                    return;
                }

                List<SavedRecipeDto> savedRows = response.body();

                if (savedRows.isEmpty()) {
                    showToast("No saved recipes yet");
                    initFeedAdapter(new ArrayList<>(), 0);
                    return;
                }

                StringBuilder ids = new StringBuilder("in.(");
                for (int i = 0; i < savedRows.size(); i++) {
                    if (i > 0) {
                        ids.append(",");
                    }
                    ids.append(savedRows.get(i).recipeId);
                }
                ids.append(")");

                api.getRecipesByIds(
                        ids.toString(),
                        "id,author_id,title,description,video_url,thumbnail_url,tags,prep_time_minutes,cook_time_minutes,estimated_calories,created_at"
                ).enqueue(new Callback<List<RecipeDto>>() {
                    @Override
                    public void onResponse(Call<List<RecipeDto>> call, Response<List<RecipeDto>> response) {
                        if (!response.isSuccessful() || response.body() == null || !isAdded()) {
                            showToast("Failed to load recipe details");
                            return;
                        }

                        Map<String, RecipeDto> recipeMap = new HashMap<>();
                        for (RecipeDto dto : response.body()) {
                            recipeMap.put(dto.id, dto);
                        }

                        List<Recipe> orderedRecipes = new ArrayList<>();
                        for (SavedRecipeDto saved : savedRows) {
                            RecipeDto dto = recipeMap.get(saved.recipeId);
                            if (dto != null) {
                                orderedRecipes.add(dto.toDomain());
                            }
                        }

                        initFeedAdapter(orderedRecipes, startPosition);
                    }

                    @Override
                    public void onFailure(Call<List<RecipeDto>> call, Throwable t) {
                        showToast("Failed to load recipe details");
                    }
                });
            }

            @Override
            public void onFailure(Call<List<SavedRecipeDto>> call, Throwable t) {
                showToast("Failed to load saved recipes");
            }
        });
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisibility(true);
        }
    }
}