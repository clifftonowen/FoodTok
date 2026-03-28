package com.example.foodtok.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.foodtok.ui.MainActivity;
import com.example.foodtok.R;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Find views — like document.getElementById()
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        Button btnLogin = findViewById(R.id.btnLogin);
        ImageView btnClose = findViewById(R.id.btnClose);
        TextView tvGoToSignUp = findViewById(R.id.tvGoToSignUp);

        // Close button — returns to previous screen
        btnClose.setOnClickListener(v -> finish());

        // Login button
        btnLogin.setOnClickListener(v -> attemptLogin());

        // "Don't have an account? Sign up" — opens SignupActivity
        tvGoToSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // close login so user doesn't stack up screens
        });
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Basic validation
        if (TextUtils.isEmpty(email)) {
            showError("Please enter your email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showError("Please enter your password");
            return;
        }

        // TODO: Connect to Spring Boot / Supabase auth later
        // For now, mock login — any email/password works
        mockLogin(email, password);
    }

    private void mockLogin(String email, String password) {
        // Simulate successful login
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