package com.example.foodtok.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.util.ApiClient;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileUserFragment extends Fragment {
    private RecyclerView rvProfileRecipes;
    private TextView tabMyRecipes, tabSaved, tvFollowerCount, tvFollowingCount, userName, btnLogout, tvDisplayName, tvRecipeCount;

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

        userName = view.findViewById(R.id.tvUsername);
        btnLogout = view.findViewById(R.id.btnLogout);
        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvRecipeCount = view.findViewById(R.id.tvRecipeCount);

        if (AuthManager.getInstance().getCurrentUser() != null) {
            userName.setText(AuthManager.getInstance().getCurrentUser().getUsername());
            tvDisplayName.setText(AuthManager.getInstance().getCurrentUser().getUsername());
        } else {
            userName.setText("Guest");
        }

        btnLogout.setOnClickListener(v -> {
            AuthServiceProvider.getAuthService().logout();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        //GridLayoutManager
        rvProfileRecipes.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new ProfileRecipeAdapter(myRecipes);
        rvProfileRecipes.setAdapter(adapter);

        tabMyRecipes.setOnClickListener(v -> {
            switchTab(true);
        });

        tabSaved.setOnClickListener(v -> {
            switchTab(false);
        });

        return view;



    }

    private void switchTab(boolean showMyRecipes) {
        isMyRecipesTab = showMyRecipes;

        // Update tab styling (like toggling active className in React)
        tabMyRecipes.setTextColor(ContextCompat.getColor(getContext(),
                showMyRecipes ? R.color.foodtok_text_primary : R.color.foodtok_text_secondary));
        tabMyRecipes.setTypeface(null, showMyRecipes ? Typeface.BOLD : Typeface.NORMAL);

        tabSaved.setTextColor(ContextCompat.getColor(getContext(),
                showMyRecipes ? R.color.foodtok_text_secondary : R.color.foodtok_text_primary));
        tabSaved.setTypeface(null, showMyRecipes ? Typeface.NORMAL : Typeface.BOLD);

        // Swap data — like setting setState with different array
        adapter.updateData(showMyRecipes ? myRecipes : savedRecipes);
    }


    private void fetchProfileStats() {
        String userId = AuthManager.getInstance().getCurrentUser().getId();
        SupabaseApi api = ApiClient.getSupabaseApi();

        // Follower count — people whose following_id = me
        api.getFollowers("eq." + userId, "follower_id").enqueue(new Callback<List<FollowDto>>() {
            @Override
            public void onResponse(Call<List<FollowDto>> call, Response<List<FollowDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvFollowerCount.setText(String.valueOf(response.body().size()));
                }
            }
            @Override
            public void onFailure(Call<List<FollowDto>> call, Throwable t) {}
        });

        // Following count — people whose follower_id = me
        api.getFollowing("eq." + userId, "following_id").enqueue(new Callback<List<FollowDto>>() {
            @Override
            public void onResponse(Call<List<FollowDto>> call, Response<List<FollowDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvFollowingCount.setText(String.valueOf(response.body().size()));
                }
            }
            @Override
            public void onFailure(Call<List<FollowDto>> call, Throwable t) {}
        });

        // Recipe count — recipes where author_id = me
        // Recipe count
        api.getRecipesByAuthor("eq." + userId, "id").enqueue(new Callback<List<RecipeDto>>() {
            @Override
            public void onResponse(Call<List<RecipeDto>> call, Response<List<RecipeDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvRecipeCount.setText(String.valueOf(response.body().size()));
                }
            }
            @Override
            public void onFailure(Call<List<RecipeDto>> call, Throwable t) {}
        });
    }



}