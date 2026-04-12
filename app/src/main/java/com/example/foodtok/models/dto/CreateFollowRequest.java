package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

public class CreateFollowRequest {
    @SerializedName("follower_id")
    private final String followerId;

    @SerializedName("following_id")
    private final String followingId;

    public CreateFollowRequest(String followerId, String followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
    }

}
