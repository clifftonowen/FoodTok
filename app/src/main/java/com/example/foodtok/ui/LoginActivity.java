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
import com.example.foodtok.auth.AuthCallback;
import com.example.foodtok.auth.AuthServiceProvider;
import com.example.foodtok.models.User;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private TextView tvError;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        btnLogin = findViewById(R.id.btnLogin);
        ImageView btnClose = findViewById(R.id.btnClose);
        TextView tvGoToSignUp = findViewById(R.id.tvGoToSignUp);

        btnClose.setOnClickListener(v -> finish());
        btnLogin.setOnClickListener(v -> attemptLogin());

        tvGoToSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            showError("Please enter your email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showError("Please enter your password");
            return;
        }

        // Disable button while loading (prevent double-tap)
        btnLogin.setEnabled(false);
        hideError();

        // This calls SupabaseAuthService (or MockAuthService)
        // LoginActivity doesn't know which — loose coupling
        AuthServiceProvider.getAuthService().login(email, password,
                new AuthCallback() {
                    @Override
                    public void onSuccess(User user) {
                        // Runs on background thread, switch to UI thread
                        runOnUiThread(() -> {
                            // Go to main screen
                            Intent intent = new Intent(LoginActivity.this,
                                    MainActivity.class);
                            // Clear back stack so user can't "back" to login
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        // Also runs on background thread
                        runOnUiThread(() -> {
                            showError(message);
                            btnLogin.setEnabled(true);
                        });
                    }
                });
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(TextView.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(TextView.GONE);
    }
}