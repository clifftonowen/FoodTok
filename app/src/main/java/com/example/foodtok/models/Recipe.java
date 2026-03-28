package com.example.foodtok.models;

import java.util.List;

public class Recipe {
    private String id;
    private String title;
    private String videoUrl;
    private List<String> tags;
    private List<String> ingredients;

    public Recipe(String id, String title, String videoUrl, List<String> tags, List<String> ingredients) {
        this.id = id;
        this.title = title;
        this.videoUrl = videoUrl;
        this.tags = tags;
        this.ingredients = ingredients;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
}
