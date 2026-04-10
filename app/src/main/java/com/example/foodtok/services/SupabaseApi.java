package com.example.foodtok.services;

import com.example.foodtok.models.dto.CommentDto;
import com.example.foodtok.models.dto.CreateCommentRequest;
import com.example.foodtok.models.dto.CreateInteractionRequest;
import com.example.foodtok.models.dto.IngredientDto;
import com.example.foodtok.models.dto.InteractionDto;
import com.example.foodtok.models.dto.RecipeDto;
import com.example.foodtok.models.dto.UpdateProfileRequest;
import com.example.foodtok.models.dto.UploadRecipeRequest;
import com.example.foodtok.models.dto.UserDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** Retrofit interface for Supabase PostgREST CRUD operations. */
public interface SupabaseApi {

  // ── Recipes ──────────────────────────────────────────────────────────

  /** Fetches recipes with nested ingredients and author. */
  @GET("recipes")
  Call<List<RecipeDto>> getRecipes(
      @Query("select") String select,
      @Query("order") String order,
      @Header("Range") String range
  );

  /** Fetches a single recipe by ID filter ({@code id=eq.<uuid>}). */
  @GET("recipes")
  Call<List<RecipeDto>> getRecipeById(
      @Query("id") String idFilter,
      @Query("select") String select
  );

  /** Creates a new recipe row. */
  @POST("recipes")
  Call<List<RecipeDto>> createRecipe(@Body UploadRecipeRequest request);

  // ── Comments ─────────────────────────────────────────────────────────

  /** Fetches comments for a recipe with nested author join. */
  @GET("comments")
  Call<List<CommentDto>> getComments(
      @Query("recipe_id") String recipeIdFilter,
      @Query("select") String select,
      @Query("order") String order
  );

  /** Creates a new comment. */
  @POST("comments")
  Call<List<CommentDto>> createComment(@Body CreateCommentRequest request);

  // ── Profiles ─────────────────────────────────────────────────────────

  /** Updates a user's profile (avatar, preferences, allergens). */
  @PATCH("profiles")
  Call<List<UserDto>> updateProfile(
      @Query("id") String idFilter,
      @Body UpdateProfileRequest request
  );

  // ── Ingredients ──────────────────────────────────────────────────────

  /** Fetches all ingredients for Trie autocomplete. */
  @GET("ingredients")
  Call<List<IngredientDto>> getAllIngredients(
      @Query("select") String select,
      @Query("order") String order
  );

  // ── Interactions ─────────────────────────────────────────────────────

  /** Checks if a specific interaction exists (e.g. like or save). */
  @GET("interactions")
  Call<List<InteractionDto>> getInteractions(
      @Query("user_id") String userIdFilter,
      @Query("recipe_id") String recipeIdFilter,
      @Query("type") String typeFilter
  );

  /** Creates a new interaction (like, save, not_interested, view). */
  @POST("interactions")
  Call<List<InteractionDto>> createInteraction(
      @Body CreateInteractionRequest request
  );

  /** Deletes an interaction by user, recipe, and type filters. */
  @DELETE("interactions")
  Call<Void> deleteInteraction(
      @Query("user_id") String userIdFilter,
      @Query("recipe_id") String recipeIdFilter,
      @Query("type") String typeFilter
  );
}
