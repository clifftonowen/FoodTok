package com.example.foodtok.auth;

import com.example.foodtok.models.User;
import com.example.foodtok.models.dto.AuthResponse;
import com.example.foodtok.models.dto.LoginRequest;
import com.example.foodtok.models.dto.SignUpRequest;
import com.example.foodtok.services.SupabaseAuthApi;
import com.example.foodtok.util.ApiClient;
import com.example.foodtok.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Production {@link IAuthService} implementation using Supabase GoTrue via Retrofit. */
public class SupabaseAuthService implements IAuthService {

    private final SupabaseAuthApi authApi;

    public SupabaseAuthService() {
        this.authApi = ApiClient.getAuthClient().create(SupabaseAuthApi.class);
    }

    @Override
    public void signUp(String username, String email, String password,
                       AuthCallback callback) {

        SignUpRequest request = new SignUpRequest(email, password, username);

    authApi.signUp(request).enqueue(new Callback<AuthResponse>() {
      @Override
      public void onResponse(Call<AuthResponse> call,
                                   Response<AuthResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    handleAuthSuccess(response.body(), callback);
                } else {
                    callback.onError("Sign up failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    @Override
    public void login(String email, String password,
                      AuthCallback callback) {

        LoginRequest request = new LoginRequest(email, password);

        authApi.login("password", request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call,
                                   Response<AuthResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    handleAuthSuccess(response.body(), callback);
                } else {
                    callback.onError("Login failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    @Override
    public void logout() {
        // Clear local session first
        SessionManager.getInstance().clearSession();
        AuthManager.getInstance().logout();
    }

  // Shared logic for both signup and login success
  private void handleAuthSuccess(AuthResponse authResponse,
                                   AuthCallback callback) {

        AuthResponse.AuthUser authUser = authResponse.getUser();

        // 1. Save JWT to disk (survives app restart)
        SessionManager.getInstance().saveSession(
                authResponse.getAccessToken(),
                authResponse.getRefreshToken(),
                authUser.getId(),
                authUser.getUsername()
        );

        // 2. Create domain User object
        User user = new User(
                authUser.getId(),
                authUser.getUsername(),
                authUser.getEmail()
        );

        // 3. Save to in-memory AuthManager
        AuthManager.getInstance().login(user);

        // 4. Tell the caller it worked
        callback.onSuccess(user);
    }
}