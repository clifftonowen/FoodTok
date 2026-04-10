package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for a row from the {@code distinct_tags} view, which exposes
 * every distinct tag across all recipes for Trie autocomplete.
 */
public class TagDto {

  @SerializedName("name")
  public String name;
}