package com.example.foodtok.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.foodtok.R;

public class SignupActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Find views
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        Button btnSignUp = findViewById(R.id.btnSignUp);
        ImageView btnClose = findViewById(R.id.btnClose);
        TextView tvGoToLogin = findViewById(R.id.tvGoToLogIn);

        // Close button — returns to previous screen
        btnClose.setOnClickListener(v -> finish());

        // Sign up button
        btnSignUp.setOnClickListener(v -> attemptSignUp());

        // "Already have an account? Log in" — opens LoginActivity
        tvGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void attemptSignUp() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(username)) {
            showError("Please enter a username");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            showError("Please enter your email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showError("Please enter a password");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        // TODO: Connect to Spring Boot / Supabase auth later
        mockSignUp(username, email, password);
    }

    private void mockSignUp(String username, String email, String password) {
        // Simulate successful signup
        // Later this will call your Spring Boot API via Retrofit
        hideError();

        // Return to previous screen (the feed)
        finish();
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(TextView.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(TextView.GONE);
    }
}