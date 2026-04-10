package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

public class CreateFollowRequest {
    @SerializedName("followers_id")
    private final String followersId;

    @SerializedName("following_id")
    private final String followingId;

    public CreateFollowRequest(String followersId, String followingId) {
        this.followersId = followersId;
        this.followingId = followingId;
    }

}
