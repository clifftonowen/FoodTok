package com.example.foodtok.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodtok.R;
import com.example.foodtok.adapters.ProfileRecipeAdapter;
import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.auth.AuthServiceProvider;
import com.example.foodtok.models.dto.FollowDto;
import com.example.foodtok.models.dto.RecipeDto;
import com.example.foodtok.models.dto.SavedRecipeDto;
import com.example.foodtok.models.dto.UserDto;
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.util.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileUserFragment extends Fragment {
    private RecyclerView rvProfileRecipes;
    private ImageView ivProfilePic;
    private TextView tabMyRecipes, tabSaved, tvFollowerCount, tvFollowingCount, userName, tvDisplayName, tvRecipeCount;
    private View llFollowers, llFollowing;
    private View tabIndicator;
    private int tabWidth = 0;

    private View layoutProfileContent;
    private View drawerOverlay;
    private View settingsDrawer;
    private ImageView btnSettings;
    private TextView drawerLogout;
    private TextView drawerManageProfile;

    private TextView drawerManagePreferences;
    private TextView tvBio;
    private boolean isDrawerOpen = false;

    private boolean isMyRecipesTab = true;
    private List<RecipeDto> myRecipes = new ArrayList<>();
    private List<RecipeDto> savedRecipes = new ArrayList<>();

    private ProfileRecipeAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_user, container, false);

        rvProfileRecipes = view.findViewById(R.id.rvProfileRecipes);
        tabMyRecipes = view.findViewById(R.id.tabMyRecipes);
        tabSaved = view.findViewById(R.id.tabSaved);
        tvFollowerCount = view.findViewById(R.id.tvFollowerCount);
        tvFollowingCount = view.findViewById(R.id.tvFollowingCount);
        tabIndicator = view.findViewById(R.id.tabIndicator);


        userName = view.findViewById(R.id.tvUsername);
        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvRecipeCount = view.findViewById(R.id.tvRecipeCount);
        ivProfilePic = view.findViewById(R.id.ivProfilePic);
        llFollowers = view.findViewById(R.id.llFollowers);
        llFollowing = view.findViewById(R.id.llFollowing);

        layoutProfileContent = view.findViewById(R.id.layoutProfileContent);
        drawerOverlay = view.findViewById(R.id.drawerOverlay);
        settingsDrawer = view.findViewById(R.id.settingsDrawer);
        btnSettings = view.findViewById(R.id.btnSettings);
        drawerLogout = view.findViewById(R.id.drawerLogout);
        drawerManageProfile = view.findViewById(R.id.drawerManageProfile);
        drawerManagePreferences = view.findViewById(R.id.drawerManagePreferences);
        tvBio = view.findViewById(R.id.tvBio);

        // Position drawer off-screen to the right before first draw
        settingsDrawer.post(() -> settingsDrawer.setTranslationX(settingsDrawer.getWidth()));

        // Profile picture entrance: scale from 0 with overshoot
        ivProfilePic.setScaleX(0f);
        ivProfilePic.setScaleY(0f);
        ivProfilePic.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setStartDelay(100)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();

        // Set tab indicator width to exactly one tab after layout
        tabMyRecipes.post(() -> {
            tabWidth = tabMyRecipes.getWidth();
            ViewGroup.LayoutParams lp = tabIndicator.getLayoutParams();
            lp.width = tabWidth;
            tabIndicator.setLayoutParams(lp);
        });

        if (AuthManager.getInstance().getCurrentUser() != null) {
            userName.setText(AuthManager.getInstance().getCurrentUser().getUsername());
            tvDisplayName.setText(AuthManager.getInstance().getCurrentUser().getUsername());
        } else {
            userName.setText("Guest");
        }

        btnSettings.setOnClickListener(v -> openDrawer());

        drawerOverlay.setOnClickListener(v -> closeDrawer());
        drawerManagePreferences.setOnClickListener(v -> {
            closeDrawer();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right, R.anim.slide_out_left,
                            R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.fragmentContainer, new ManagePreferencesFragment())
                    .addToBackStack(null)
                    .commit();
        });

        drawerManageProfile.setOnClickListener(v -> {
            closeDrawer();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right, R.anim.slide_out_left,
                            R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.fragmentContainer, new ManageProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        drawerLogout.setOnClickListener(v -> {
            AuthServiceProvider.getAuthService().logout();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (isDrawerOpen) {
                            closeDrawer();
                        } else {
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                }
        );

        llFollowers.setOnClickListener(v -> {
            String userId = AuthManager.getInstance().getCurrentUser().getId();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right, R.anim.slide_out_left,
                            R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.fragmentContainer,
                            FollowListFragment.newInstance(userId, FollowListFragment.MODE_FOLLOWERS))
                    .addToBackStack(null)
                    .commit();
        });

        llFollowing.setOnClickListener(v -> {
            String userId = AuthManager.getInstance().getCurrentUser().getId();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right, R.anim.slide_out_left,
                            R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.fragmentContainer,
                            FollowListFragment.newInstance(userId, FollowListFragment.MODE_FOLLOWING))
                    .addToBackStack(null)
                    .commit();
        });

        rvProfileRecipes.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new ProfileRecipeAdapter(myRecipes);
        rvProfileRecipes.setAdapter(adapter);

        adapter.setOnRecipeClickListener(position -> {
            String userId = AuthManager.getInstance().getCurrentUser() != null
                    ? AuthManager.getInstance().getCurrentUser().getId() : "";

            Fragment nextFragment;
            if (isMyRecipesTab) {
                nextFragment = ProfileSavedFeedFragment.newInstance(position, ProfileSavedFeedFragment.MODE_MY_RECIPES, userId);
            } else {
                nextFragment = ProfileSavedFeedFragment.newInstance(position, ProfileSavedFeedFragment.MODE_SAVED, userId);
            }

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right, R.anim.slide_out_left,
                            R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.fragmentContainer, nextFragment)
                    .addToBackStack(null)
                    .commit();
        });

        tabMyRecipes.setOnClickListener(v -> switchTab(true));
        tabSaved.setOnClickListener(v -> switchTab(false));

        fetchProfileStats();
        fetchMyRecipes();
        fetchSavedRecipes();
        fetchAvatar();
        switchTab(isMyRecipesTab);

        return view;
    }

    private void openDrawer() {
        if (isDrawerOpen) return;
        isDrawerOpen = true;

        float contentShift = -settingsDrawer.getWidth() * 0.35f;

        drawerOverlay.setVisibility(View.VISIBLE);

        layoutProfileContent.animate()
                .translationX(contentShift)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        settingsDrawer.animate()
                .translationX(0f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        drawerOverlay.animate()
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void closeDrawer() {
        if (!isDrawerOpen) return;
        isDrawerOpen = false;

        layoutProfileContent.animate()
                .translationX(0f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        settingsDrawer.animate()
                .translationX(settingsDrawer.getWidth())
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        drawerOverlay.animate()
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> drawerOverlay.setVisibility(View.GONE))
                .start();
    }

    private void animateStatCount(TextView tv) {
        tv.setScaleX(0f);
        tv.setScaleY(0f);
        tv.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(2f))
                .start();
    }

    private void switchTab(boolean showMyRecipes) {
        isMyRecipesTab = showMyRecipes;

        tabMyRecipes.setTextColor(ContextCompat.getColor(getContext(),
                showMyRecipes ? R.color.foodtok_text_primary : R.color.foodtok_text_secondary));
        tabMyRecipes.setTypeface(null, showMyRecipes ? Typeface.BOLD : Typeface.NORMAL);

        tabSaved.setTextColor(ContextCompat.getColor(getContext(),
                showMyRecipes ? R.color.foodtok_text_secondary : R.color.foodtok_text_primary));
        tabSaved.setTypeface(null, showMyRecipes ? Typeface.NORMAL : Typeface.BOLD);

        // Slide the green underline indicator to the active tab
        if (tabWidth > 0) {
            tabIndicator.animate()
                    .translationX(showMyRecipes ? 0f : tabWidth)
                    .setDuration(200)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        adapter.updateData(showMyRecipes ? myRecipes : savedRecipes);
    }

    private void fetchSavedRecipes() {
        String userId = AuthManager.getInstance().getCurrentUser().getId();
        SupabaseApi api = ApiClient.getSupabaseApi();

        api.getSavedRecipes(
                "eq." + userId,
                "user_id,recipe_id,created_at",
                "created_at.desc"
        ).enqueue(new Callback<List<SavedRecipeDto>>() {
            @Override
            public void onResponse(Call<List<SavedRecipeDto>> call, Response<List<SavedRecipeDto>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                List<SavedRecipeDto> savedRows = response.body();
                if (savedRows.isEmpty()) {
                    savedRecipes.clear();
                    if (!isMyRecipesTab) {
                        adapter.updateData(savedRecipes);
                    }
                    return;
                }

                StringBuilder idsBuilder = new StringBuilder("in.(");
                for (int i = 0; i < savedRows.size(); i++) {
                    idsBuilder.append(savedRows.get(i).recipeId);
                    if (i < savedRows.size() - 1) {
                        idsBuilder.append(",");
                    }
                }
                idsBuilder.append(")");

                api.getRecipesByIds(
                        idsBuilder.toString(),
                        "id,author_id,title,description,video_url,thumbnail_url,tags,prep_time_minutes,cook_time_minutes,estimated_calories,created_at"
                ).enqueue(new Callback<List<RecipeDto>>() {
                    @Override
                    public void onResponse(Call<List<RecipeDto>> call, Response<List<RecipeDto>> response) {
                        if (!response.isSuccessful() || response.body() == null) return;

                        List<RecipeDto> fetchedRecipes = response.body();

                        Map<String, RecipeDto> recipeMap = new HashMap<>();
                        for (RecipeDto recipe : fetchedRecipes) {
                            recipeMap.put(recipe.id, recipe);
                        }

                        List<RecipeDto> orderedRecipes = new ArrayList<>();
                        for (SavedRecipeDto saved : savedRows) {
                            RecipeDto recipe = recipeMap.get(saved.recipeId);
                            if (recipe != null) {
                                orderedRecipes.add(recipe);
                            }
                        }

                        savedRecipes.clear();
                        savedRecipes.addAll(orderedRecipes);

                        if (!isMyRecipesTab) {
                            adapter.updateData(savedRecipes);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<RecipeDto>> call, Throwable t) {
                    }
                });
            }

            @Override
            public void onFailure(Call<List<SavedRecipeDto>> call, Throwable t) {
            }
        });
    }

    private void fetchMyRecipes() {
        String userId = AuthManager.getInstance().getCurrentUser().getId();
        SupabaseApi api = ApiClient.getSupabaseApi();

        api.getRecipesByAuthor(
                "eq." + userId,
                "id,author_id,title,description,video_url,thumbnail_url,tags,prep_time_minutes,cook_time_minutes,estimated_calories,created_at"
        ).enqueue(new Callback<List<RecipeDto>>() {
            @Override
            public void onResponse(Call<List<RecipeDto>> call, Response<List<RecipeDto>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                myRecipes.clear();
                myRecipes.addAll(response.body());
                if (isMyRecipesTab) {
                    adapter.updateData(myRecipes);
                }
            }

            @Override
            public void onFailure(Call<List<RecipeDto>> call, Throwable t) {}
        });
    }

    private void fetchAvatar() {
        String userId = AuthManager.getInstance().getCurrentUser().getId();
        ApiClient.getSupabaseApi().getProfiles("eq." + userId, "id,avatar_url,bio")
                .enqueue(new Callback<List<UserDto>>() {
                    @Override
                    public void onResponse(Call<List<UserDto>> call, Response<List<UserDto>> response) {
                        if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) return;
                        UserDto profile = response.body().get(0);

                        if (!TextUtils.isEmpty(profile.avatarUrl)) {
                            Glide.with(ProfileUserFragment.this)
                                    .load(profile.avatarUrl)
                                    .circleCrop()
                                    .transition(DrawableTransitionOptions.withCrossFade(300))
                                    .into(ivProfilePic);
                        }

                        if (!TextUtils.isEmpty(profile.bio)) {
                            tvBio.setText(profile.bio);
                            tvBio.setVisibility(View.VISIBLE);
                        } else {
                            tvBio.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<UserDto>> call, Throwable t) {}
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchAvatar();
    }

    private void fetchProfileStats() {
        String userId = AuthManager.getInstance().getCurrentUser().getId();
        SupabaseApi api = ApiClient.getSupabaseApi();

        api.getFollowers("eq." + userId, "follower_id").enqueue(new Callback<List<FollowDto>>() {
            @Override
            public void onResponse(Call<List<FollowDto>> call, Response<List<FollowDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvFollowerCount.setText(String.valueOf(response.body().size()));
                    animateStatCount(tvFollowerCount);
                }
            }
            @Override
            public void onFailure(Call<List<FollowDto>> call, Throwable t) {}
        });

        api.getFollowing("eq." + userId, "following_id").enqueue(new Callback<List<FollowDto>>() {
            @Override
            public void onResponse(Call<List<FollowDto>> call, Response<List<FollowDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvFollowingCount.setText(String.valueOf(response.body().size()));
                    animateStatCount(tvFollowingCount);
                }
            }
            @Override
            public void onFailure(Call<List<FollowDto>> call, Throwable t) {}
        });

        api.getRecipesByAuthor("eq." + userId, "id").enqueue(new Callback<List<RecipeDto>>() {
            @Override
            public void onResponse(Call<List<RecipeDto>> call, Response<List<RecipeDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvRecipeCount.setText(String.valueOf(response.body().size()));
                    animateStatCount(tvRecipeCount);
                }
            }
            @Override
            public void onFailure(Call<List<RecipeDto>> call, Throwable t) {}
        });
    }
}
