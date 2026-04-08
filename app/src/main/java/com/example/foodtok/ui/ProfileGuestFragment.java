package com.example.foodtok.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.foodtok.R;
import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.auth.AuthServiceProvider;

public class ProfileGuestFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_guest, container, false);

        Button btnProfileSignUp = view.findViewById(R.id.btnProfileSignUp);
        Button btnProfileLogin = view.findViewById(R.id.btnProfileLogin);

        btnProfileSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SignupActivity.class);
            startActivity(intent);
        });

        btnProfileLogin.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
        });

        return view;
        }
}