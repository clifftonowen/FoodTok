package com.example.foodtok.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/** Domain model for a user with interest profile, blacklisted ingredients, and preferences. */
public class User {
  private final String id;
  private String username;
  private final String email;
  private String avatarUrl;
  private final Map<String, Integer> interestProfile;
  private final List<String> blacklistedIngredients;

  public User(String id, String username, String email){
    this.id = id;
    this.username = username;
    this.email = email;
    this.interestProfile = new HashMap<>();
    this.blacklistedIngredients = new ArrayList<>();
  }

  //Getters (only what's needed externally)
  public String getId(){
    return id;
  }

  public String getUsername(){
    return username;
  }

  // Setter (users can change display name)
  public void setUsername(String username) {
    if (username == null || username.trim().isEmpty()) {
      throw new IllegalArgumentException("Username cannot be empty");
    }
    this.username = username.trim();
  }

  // Interest Profile (Tell, Don't Ask)
  // Instead of exposing the HashMap, provide specific actions

  public void updateInterestScore(String tag, int points) {
    int currentScore = interestProfile.getOrDefault(tag, 0);
    // Previous score + additional points (e.g. like = +10, save = +15)
    interestProfile.put(tag, currentScore + points);
  }

  public int getScoreForTag(String tag) {
    return interestProfile.getOrDefault(tag, 0);
  }

  public List<String> getTopPreferences(int limit) {
    // Subtype Polymorphism: List<Entry> backed by ArrayList
    List<Map.Entry<String, Integer>> entries =
        new ArrayList<>(interestProfile.entrySet());
    entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

    List<String> topTags = new ArrayList<>();
    for (int i = 0; i < Math.min(limit, entries.size()); i++) {
      topTags.add(entries.get(i).getKey());
    }
    return topTags;
  }

  // Blacklisted Ingredients (Tell, Don't Ask)
  // Instead of exposing the list, provide specific actions
  public void addBlacklistedIngredient(String ingredient) {
    String lower = ingredient.toLowerCase();
    if (!blacklistedIngredients.contains(lower)) {
      blacklistedIngredients.add(lower);
    }
  }
  public void removeBlacklistedIngredient(String ingredient) {
    blacklistedIngredients.remove(ingredient.toLowerCase());
  }

  // Other classes ask the User, not inspect the list
  public boolean isIngredientBlacklisted(String ingredient) {
    return blacklistedIngredients.contains(ingredient.toLowerCase());
  }

  public int getBlacklistedCount() {
    return blacklistedIngredients.size();
  }

  /** Returns the avatar URL, or {@code null} if not set. */
  public String getAvatarUrl() {
    return avatarUrl;
  }

  /**
   * Sets the user's avatar URL.
   *
   * @param avatarUrl the URL to the avatar image
   */
  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  /**
   * Returns a copy of the interest profile map for serialization.
   *
   * @return map of tag names to preference scores
   */
  public Map<String, Integer> getInterestProfile() {
    return new HashMap<>(interestProfile);
  }

  /**
   * Returns a copy of the blacklisted ingredients list for serialization.
   *
   * @return list of blacklisted ingredient names
   */
  public List<String> getBlacklistedIngredients() {
    return new ArrayList<>(blacklistedIngredients);
  }









}
