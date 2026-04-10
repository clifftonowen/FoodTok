package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

public class FollowDto {
    @SerializedName("follower_id")
    private String followerId;

    @SerializedName("following_id")
    private String followingId;

    @SerializedName("created_at")
    private String createdAt;

    public String getFollowerId() { return followerId; }
    public String getFollowingId() { return followingId; }
}