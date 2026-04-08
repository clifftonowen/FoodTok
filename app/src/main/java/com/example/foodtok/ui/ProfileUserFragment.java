package com.example.foodtok.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.foodtok.R;
import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.auth.AuthServiceProvider;

public class ProfileUserFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile_user, container, false);

        TextView userName = view.findViewById(R.id.tvUsername);
        TextView btnLogout = view.findViewById(R.id.btnLogout);
        TextView tvDisplayName = view.findViewById(R.id.tvDisplayName);

        if (AuthManager.getInstance().getCurrentUser() != null) {
            userName.setText(AuthManager.getInstance().getCurrentUser().getUsername());
            tvDisplayName.setText(AuthManager.getInstance().getCurrentUser().getUsername());
        } else {
            userName.setText("Guest");
        }

        btnLogout.setOnClickListener(v -> {
            // 1. Clear session + auth state
            AuthServiceProvider.getAuthService().logout();

            // 2. Restart app at MainActivity (profile tab will now show guest)
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }
}