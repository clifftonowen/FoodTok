package com.example.foodtok.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.foodtok.R;
import com.example.foodtok.auth.AuthCallback;
import com.example.foodtok.auth.AuthServiceProvider;
import com.example.foodtok.models.User;

/**
 * Signup screen for new user registration via the active
 * {@link com.example.foodtok.auth.IAuthService}.
 */
public class SignupActivity extends AppCompatActivity {

  private EditText etUsername, etEmail, etPassword;
  private TextView tvError;
  private Button btnSignUp;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_signup);

    etUsername = findViewById(R.id.etUsername);
    etEmail = findViewById(R.id.etEmail);
    etPassword = findViewById(R.id.etPassword);
    tvError = findViewById(R.id.tvError);
    btnSignUp = findViewById(R.id.btnSignUp);

    ImageView btnClose = findViewById(R.id.btnClose);
    TextView tvGoToLogin = findViewById(R.id.tvGoToLogIn);

    btnClose.setOnClickListener(v -> finish());
    btnSignUp.setOnClickListener(v -> attemptSignUp());

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

    btnSignUp.setEnabled(false);

    AuthServiceProvider.getAuthService().signUp(username, email, password, new AuthCallback() {
      @Override
      public void onSuccess(User user) {
        runOnUiThread(() -> {
          Intent intent = new Intent(
              SignupActivity.this, OnboardingActivity.class);
          intent.setFlags(
              Intent.FLAG_ACTIVITY_NEW_TASK
                  | Intent.FLAG_ACTIVITY_CLEAR_TASK);
          startActivity(intent);
          finish();
        });
      }

      @Override
      public void onError(String message) {
        runOnUiThread(() -> {
          showError(message);
          btnSignUp.setEnabled(true);
        });
      }
    });
  }

  private void showError(String message) {
    tvError.setText(message);
    tvError.setVisibility(View.VISIBLE);
  }

  private void hideError() {
    tvError.setVisibility(View.GONE);
  }
}