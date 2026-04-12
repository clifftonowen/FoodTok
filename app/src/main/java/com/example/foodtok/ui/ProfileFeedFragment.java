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
import com.example.foodtok.models.dto.InteractionDto;
import com.example.foodtok.models.dto.RecipeDto;
import com.example.foodtok.services.InteractionCallback;
import com.example.foodtok.services.InteractionServiceProvider;
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.util.ApiClient;
import com.example.foodtok.util.FeedVideoPlayerPool;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFeedFragment extends Fragment {

    private static final String ARG_START_POSITION = "startPosition";
    private static final String ARG_FEED_TYPE = "feedType";       // "my_recipes" or "saved"
    private static final String ARG_USER_ID = "userId";

    private ViewPager2 viewPager;
    private FeedAdapter feedAdapter;
    private FeedVideoPlayerPool playerPool;
    private final List<Recipe> recipes = new ArrayList<>();

    public static ProfileFeedFragment newInstance(int startPosition, String feedType, String userId) {
        ProfileFeedFragment fragment = new ProfileFeedFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_START_POSITION, startPosition);
        args.putString(ARG_FEED_TYPE, feedType);
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

        playerPool = new FeedVideoPlayerPool(requireContext());

        feedAdapter = new FeedAdapter(recipes, new OnRecipeInteractionListener() {
            @Override
            public void onLikeClicked(Recipe recipe) {
                InteractionServiceProvider.getInteractionService()
                    .likeRecipe(recipe.getId(), new InteractionCallback() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override public void onSuccess() { feedAdapter.notifyDataSetChanged(); }
                        @Override public void onError(String message) {
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
                CommentsFragment.newInstance(recipe.getId())
                    .show(getChildFragmentManager(), "comments");
            }

            @Override
            public void onSaveClicked(Recipe recipe) {
                InteractionServiceProvider.getInteractionService()
                    .saveRecipe(recipe.getId(), new InteractionCallback() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override public void onSuccess() { feedAdapter.notifyDataSetChanged(); }
                        @Override public void onError(String message) {
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
                        @Override public void onSuccess() { feedAdapter.notifyDataSetChanged(); }
                        @Override public void onError(String message) {
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
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        loadRecipes();
        return view;
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

    @SuppressLint("NotifyDataSetChanged")
    private void loadRecipes() {
        String feedType = getArguments().getString(ARG_FEED_TYPE);
        String userId = getArguments().getString(ARG_USER_ID);
        int startPosition = getArguments().getInt(ARG_START_POSITION, 0);
        SupabaseApi api = ApiClient.getSupabaseApi();

        if ("my_recipes".equals(feedType)) {
            api.getRecipesByAuthor("eq." + userId, "*,author:profiles(username)")
                    .enqueue(new Callback<List<RecipeDto>>() {
                        @Override
                        public void onResponse(Call<List<RecipeDto>> call, Response<List<RecipeDto>> response) {
                            if (response.isSuccessful() && response.body() != null && getActivity() != null) {
                                getActivity().runOnUiThread(() ->
                                    initFeedWithData(response.body(),
                                        startPosition));
                            }
                        }
                        @Override
                        public void onFailure(Call<List<RecipeDto>> call,
                            Throwable t) {}
                    });
        } else {
            loadSavedRecipes(userId, startPosition);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadSavedRecipes(String userId, int startPosition) {
        SupabaseApi api = ApiClient.getSupabaseApi();

        api.getInteractions("eq." + userId, null, "eq.save")
                .enqueue(new Callback<List<InteractionDto>>() {
                    @Override
                    public void onResponse(Call<List<InteractionDto>> call,
                                           Response<List<InteractionDto>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && !response.body().isEmpty()) {
                            StringBuilder ids = new StringBuilder("in.(");
                            for (int i = 0; i < response.body().size(); i++) {
                                if (i > 0) ids.append(",");
                                ids.append(response.body().get(i).recipeId);
                            }
                            ids.append(")");

                            api.getRecipeById(ids.toString(), "*,author:profiles(username)")
                                    .enqueue(new Callback<List<RecipeDto>>() {
                                        @Override
                                        public void onResponse(Call<List<RecipeDto>> call, Response<List<RecipeDto>> response) {
                                            if (response.isSuccessful() && response.body() != null && getActivity() != null) {
                                                getActivity().runOnUiThread(() ->
                                                    initFeedWithData(
                                                        response.body(),
                                                        startPosition));
                                            }
                                        }
                                        @Override
                                        public void onFailure(Call<List<RecipeDto>> call, Throwable t) {}
                                    });
                        }
                    }
                    @Override
                    public void onFailure(Call<List<InteractionDto>> call, Throwable t) {}
                });
    }

    /**
     * Populates the feed once recipe data arrives. Mirrors the
     * HomeFragment strategy: prime the pool with recipes and the
     * starting position BEFORE setting the adapter, so the very
     * first bind already finds a ready player to attach.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void initFeedWithData(List<RecipeDto> dtos,
        int startPosition) {
        recipes.clear();
        for (RecipeDto dto : dtos) {
            recipes.add(dto.toDomain());
        }
        if (playerPool != null) {
            playerPool.setRecipes(recipes);
            int pos = startPosition > 0 ? startPosition : 0;
            playerPool.setCurrentPosition(pos);
        }
        viewPager.setAdapter(feedAdapter);
        if (startPosition > 0) {
            viewPager.setCurrentItem(startPosition, false);
        }
    }

    private void showToast(String message) {
        if (getContext() != null) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisibility(true);
        }
    }
}
