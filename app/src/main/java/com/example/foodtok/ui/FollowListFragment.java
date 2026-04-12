package com.example.foodtok.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodtok.R;
import com.example.foodtok.adapters.FollowUserAdapter;
import com.example.foodtok.models.dto.FollowDto;
import com.example.foodtok.models.dto.UserDto;
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.util.ApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FollowListFragment extends Fragment {

    public static final String MODE_FOLLOWERS = "followers";
    public static final String MODE_FOLLOWING = "following";

    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_MODE = "mode";

    private String userId;
    private String mode;

    private RecyclerView rvFollowList;
    private ProgressBar pbLoading;
    private TextView tvEmpty;
    private TextView tvTitle;

    private FollowUserAdapter adapter;
    private final List<UserDto> userList = new ArrayList<>();

    public static FollowListFragment newInstance(String userId, String mode) {
        FollowListFragment fragment = new FollowListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID, "");
            mode = getArguments().getString(ARG_MODE, MODE_FOLLOWERS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_follow_list, container, false);

        rvFollowList = view.findViewById(R.id.rvFollowList);
        pbLoading = view.findViewById(R.id.pbLoading);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvTitle = view.findViewById(R.id.tvFollowListTitle);

        tvTitle.setText(MODE_FOLLOWERS.equals(mode) ? "Followers" : "Following");

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        rvFollowList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FollowUserAdapter(userList);
        rvFollowList.setAdapter(adapter);

        adapter.setOnUserClickListener(user -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right, R.anim.slide_out_left,
                            R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.fragmentContainer, OtherUserProfileFragment.newInstance(user.id))
                    .addToBackStack(null)
                    .commit();
        });

        fetchFollowList();

        return view;
    }

    private void fetchFollowList() {
        pbLoading.setVisibility(View.VISIBLE);
        rvFollowList.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        SupabaseApi api = ApiClient.getSupabaseApi();

        if (MODE_FOLLOWERS.equals(mode)) {
            // Followers: rows where following_id = userId → we want follower_id values
            api.getFollowers("eq." + userId, "follower_id").enqueue(new Callback<List<FollowDto>>() {
                @Override
                public void onResponse(Call<List<FollowDto>> call, Response<List<FollowDto>> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        showEmpty();
                        return;
                    }
                    List<String> ids = new ArrayList<>();
                    for (FollowDto dto : response.body()) {
                        ids.add(dto.getFollowerId());
                    }
                    fetchProfiles(ids);
                }

                @Override
                public void onFailure(Call<List<FollowDto>> call, Throwable t) {
                    showEmpty();
                }
            });
        } else {
            // Following: rows where follower_id = userId → we want following_id values
            api.getFollowing("eq." + userId, "following_id").enqueue(new Callback<List<FollowDto>>() {
                @Override
                public void onResponse(Call<List<FollowDto>> call, Response<List<FollowDto>> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        showEmpty();
                        return;
                    }
                    List<String> ids = new ArrayList<>();
                    for (FollowDto dto : response.body()) {
                        ids.add(dto.getFollowingId());
                    }
                    fetchProfiles(ids);
                }

                @Override
                public void onFailure(Call<List<FollowDto>> call, Throwable t) {
                    showEmpty();
                }
            });
        }
    }

    private void fetchProfiles(List<String> ids) {
        if (ids.isEmpty()) {
            showEmpty();
            return;
        }

        StringBuilder sb = new StringBuilder("in.(");
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i));
            if (i < ids.size() - 1) sb.append(",");
        }
        sb.append(")");

        ApiClient.getSupabaseApi().getProfiles(sb.toString(), "id,username,avatar_url")
                .enqueue(new Callback<List<UserDto>>() {
                    @Override
                    public void onResponse(Call<List<UserDto>> call, Response<List<UserDto>> response) {
                        pbLoading.setVisibility(View.GONE);
                        if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                            showEmpty();
                            return;
                        }
                        userList.clear();
                        userList.addAll(response.body());
                        adapter.updateData(userList);
                        rvFollowList.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onFailure(Call<List<UserDto>> call, Throwable t) {
                        showEmpty();
                    }
                });
    }

    private void showEmpty() {
        pbLoading.setVisibility(View.GONE);
        rvFollowList.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
    }
}
