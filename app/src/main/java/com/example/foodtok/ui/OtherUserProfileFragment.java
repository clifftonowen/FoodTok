package com.example.foodtok.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodtok.R;
import com.example.foodtok.adapters.ProfileRecipeAdapter;
import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.models.dto.CreateFollowRequest;
import com.example.foodtok.models.dto.FollowDto;
import com.example.foodtok.models.dto.RecipeDto;
import com.example.foodtok.models.dto.UserDto;
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.util.ApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtherUserProfileFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";

    private String profileUserId;
    private boolean isFollowing = false;

    private ImageView ivProfilePic;
    private TextView tvUsername, tvDisplayName, tvRecipeCount, tvFollowerCount, tvFollowingCount;
    private TextView btnFollowUnfollow;
    private View llFollowers, llFollowing;
    private RecyclerView rvProfileRecipes;

    private final List<RecipeDto> recipes = new ArrayList<>();
    private ProfileRecipeAdapter adapter;

    public static OtherUserProfileFragment newInstance(String userId) {
        OtherUserProfileFragment fragment = new OtherUserProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            profileUserId = getArguments().getString(ARG_USER_ID, "");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_other_user_profile, container, false);

        ivProfilePic = view.findViewById(R.id.ivProfilePic);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvRecipeCount = view.findViewById(R.id.tvRecipeCount);
        tvFollowerCount = view.findViewById(R.id.tvFollowerCount);
        tvFollowingCount = view.findViewById(R.id.tvFollowingCount);
        btnFollowUnfollow = view.findViewById(R.id.btnFollowUnfollow);
        llFollowers = view.findViewById(R.id.llFollowers);
        llFollowing = view.findViewById(R.id.llFollowing);
        rvProfileRecipes = view.findViewById(R.id.rvProfileRecipes);

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

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        rvProfileRecipes.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new ProfileRecipeAdapter(recipes);
        rvProfileRecipes.setAdapter(adapter);

        adapter.setOnRecipeClickListener(position -> {
            Fragment nextFragment = ProfileSavedFeedFragment.newInstance(
                    position, ProfileSavedFeedFragment.MODE_MY_RECIPES, profileUserId);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right, R.anim.slide_out_left,
                            R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.fragmentContainer, nextFragment)
                    .addToBackStack(null)
                    .commit();
        });

        llFollowers.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(
                                R.anim.slide_in_right, R.anim.slide_out_left,
                                R.anim.slide_in_left, R.anim.slide_out_right)
                        .replace(R.id.fragmentContainer,
                                FollowListFragment.newInstance(profileUserId, FollowListFragment.MODE_FOLLOWERS))
                        .addToBackStack(null)
                        .commit());

        llFollowing.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(
                                R.anim.slide_in_right, R.anim.slide_out_left,
                                R.anim.slide_in_left, R.anim.slide_out_right)
                        .replace(R.id.fragmentContainer,
                                FollowListFragment.newInstance(profileUserId, FollowListFragment.MODE_FOLLOWING))
                        .addToBackStack(null)
                        .commit());

        // Follow button: scale press animation on every tap
        btnFollowUnfollow.setOnClickListener(v -> {
            v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(100)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f)
                            .setDuration(200)
                            .setInterpolator(new OvershootInterpolator(2.5f))
                            .start())
                    .start();
            toggleFollow();
        });

        fetchProfile();
        fetchRecipes();
        fetchStats();
        checkFollowState();

        return view;
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

    private void fetchProfile() {
        ApiClient.getSupabaseApi().getProfiles("eq." + profileUserId, "id,username,avatar_url")
                .enqueue(new Callback<List<UserDto>>() {
                    @Override
                    public void onResponse(Call<List<UserDto>> call, Response<List<UserDto>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            UserDto user = response.body().get(0);
                            String name = user.username != null ? user.username : "";
                            tvUsername.setText(name);
                            tvDisplayName.setText(name);
                            if (!TextUtils.isEmpty(user.avatarUrl)) {
                                Glide.with(OtherUserProfileFragment.this)
                                        .load(user.avatarUrl)
                                        .circleCrop()
                                        .transition(DrawableTransitionOptions.withCrossFade(300))
                                        .into(ivProfilePic);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<List<UserDto>> call, Throwable t) {}
                });
    }

    private void fetchRecipes() {
        ApiClient.getSupabaseApi().getRecipesByAuthor(
                "eq." + profileUserId,
                "id,author_id,title,description,video_url,thumbnail_url,tags,prep_time_minutes,cook_time_minutes,estimated_calories,created_at"
        ).enqueue(new Callback<List<RecipeDto>>() {
            @Override
            public void onResponse(Call<List<RecipeDto>> call, Response<List<RecipeDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    recipes.clear();
                    recipes.addAll(response.body());
                    tvRecipeCount.setText(String.valueOf(recipes.size()));
                    animateStatCount(tvRecipeCount);
                    adapter.updateData(recipes);
                }
            }

            @Override
            public void onFailure(Call<List<RecipeDto>> call, Throwable t) {}
        });
    }

    private void fetchStats() {
        SupabaseApi api = ApiClient.getSupabaseApi();

        api.getFollowers("eq." + profileUserId, "follower_id").enqueue(new Callback<List<FollowDto>>() {
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

        api.getFollowing("eq." + profileUserId, "following_id").enqueue(new Callback<List<FollowDto>>() {
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
    }

    private void checkFollowState() {
        String myId = AuthManager.getInstance().getCurrentUser() != null
                ? AuthManager.getInstance().getCurrentUser().getId() : "";
        if (myId.isEmpty()) return;

        ApiClient.getSupabaseApi().checkFollow("eq." + myId, "eq." + profileUserId, "follower_id")
                .enqueue(new Callback<List<FollowDto>>() {
                    @Override
                    public void onResponse(Call<List<FollowDto>> call, Response<List<FollowDto>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            isFollowing = !response.body().isEmpty();
                            updateFollowButton(isFollowing);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<FollowDto>> call, Throwable t) {}
                });
    }

    private void toggleFollow() {
        String myId = AuthManager.getInstance().getCurrentUser() != null
                ? AuthManager.getInstance().getCurrentUser().getId() : "";
        if (myId.isEmpty()) return;

        SupabaseApi api = ApiClient.getSupabaseApi();

        if (isFollowing) {
            api.unfollowUser("eq." + myId, "eq." + profileUserId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        isFollowing = false;
                        updateFollowButton(false);
                        refreshFollowerCount();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
        } else {
            api.followUser(new CreateFollowRequest(myId, profileUserId)).enqueue(new Callback<List<FollowDto>>() {
                @Override
                public void onResponse(Call<List<FollowDto>> call, Response<List<FollowDto>> response) {
                    if (response.isSuccessful()) {
                        isFollowing = true;
                        updateFollowButton(true);
                        refreshFollowerCount();
                    }
                }

                @Override
                public void onFailure(Call<List<FollowDto>> call, Throwable t) {}
            });
        }
    }

    private void updateFollowButton(boolean following) {
        if (following) {
            btnFollowUnfollow.setText("Unfollow");
            btnFollowUnfollow.setTextColor(ContextCompat.getColor(requireContext(), R.color.foodtok_red));
        } else {
            btnFollowUnfollow.setText("Follow");
            btnFollowUnfollow.setTextColor(ContextCompat.getColor(requireContext(), R.color.foodtok_green));
        }
    }

    private void refreshFollowerCount() {
        ApiClient.getSupabaseApi().getFollowers("eq." + profileUserId, "follower_id")
                .enqueue(new Callback<List<FollowDto>>() {
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
    }
}
