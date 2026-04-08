package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("refresh_token")
    private String refreshToken;

    private AuthUser user;

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public AuthUser getUser() {
        return user;
    }

    // Nested class for the "user" object in the response
    public static class AuthUser {
        private String id;
        private String email;

        @SerializedName("user_metadata")
        private UserMetadata userMetadata;

        public String getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public String getUsername() {
            if (userMetadata != null && userMetadata.username != null) {
                return userMetadata.username;
            }
            return "user_" + id.substring(0, 8);
        }
    }

    // Nested class for "user_metadata"
    public static class UserMetadata {
        private String username;
    }
}