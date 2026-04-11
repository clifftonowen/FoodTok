package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

/**
 * POST body for upserting an ingredient into the Supabase {@code ingredients}
 * table. Only the name is provided — the server-side unique constraint on
 * {@code name} combined with {@code on_conflict=name} + {@code Prefer:
 * resolution=merge-duplicates} makes this an idempotent upsert that returns
 * the existing row if the name already exists.
 */
public class CreateIngredientRequest {

  @SerializedName("name")
  public String name;

  public CreateIngredientRequest(String name) {
    this.name = name;
  }
}
