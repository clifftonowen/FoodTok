package com.example.foodtok.services;

import com.example.foodtok.models.dto.CommentDto;
import com.example.foodtok.models.dto.CreateCommentRequest;
import com.example.foodtok.models.dto.CreateFollowRequest;
import com.example.foodtok.models.dto.CreateIngredientRequest;
import com.example.foodtok.models.dto.CreateInteractionRequest;
import com.example.foodtok.models.dto.CreateRecipeIngredientRequest;
import com.example.foodtok.models.dto.CreateSavedRecipeRequest;
import com.example.foodtok.models.dto.IngredientDto;
import com.example.foodtok.models.dto.FollowDto;
import com.example.foodtok.models.dto.InteractionDto;
import com.example.foodtok.models.dto.RecipeDto;
import com.example.foodtok.models.dto.SavedRecipeDto;
import com.example.foodtok.models.dto.TagDto;
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

    /** Fetches recipes by a specific author. */
    @GET("recipes")
    Call<List<RecipeDto>> getRecipesByAuthor(
            @Query("author_id") String authorIdFilter,
            @Query("select") String select
    );

  /** Fetches a single recipe by ID filter ({@code id=eq.<uuid>}). */
  @GET("recipes")
  Call<List<RecipeDto>> getRecipeById(
      @Query("id") String idFilter,
      @Query("select") String select
  );

    @GET("recipes")
    Call<List<RecipeDto>> getRecipesByIds(
            @Query("id") String idFilter,
            @Query("select") String select
    );

  /** Creates a new recipe row. */
  @POST("recipes")
  Call<List<RecipeDto>> createRecipe(@Body UploadRecipeRequest request);

  /** Deletes a recipe by ID filter (e.g. {@code eq.<uuid>}). */
  @DELETE("recipes")
  Call<Void> deleteRecipe(@Query("id") String idFilter);

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

  /**
   * Looks up an ingredient row by exact name. Call with
   * {@code nameFilter = "eq." + name}. Returns an empty list if no such
   * row exists — callers should then POST a new one via
   * {@link #createIngredient(CreateIngredientRequest)}.
   */
  @GET("ingredients")
  Call<List<IngredientDto>> getIngredientByName(
      @Query("name") String nameFilter,
      @Query("select") String select
  );

  /**
   * Inserts a brand-new ingredient row. Caller must first verify the
   * name does not exist (via {@link #getIngredientByName}) to avoid a
   * unique-constraint violation.
   */
  @POST("ingredients")
  Call<List<IngredientDto>> createIngredient(
      @Body CreateIngredientRequest request
  );

  /** Batch-inserts rows into the {@code recipe_ingredients} join table. */
  @POST("recipe_ingredients")
  Call<Void> createRecipeIngredients(
      @Body List<CreateRecipeIngredientRequest> rows
  );

  // ── Tags ─────────────────────────────────────────────────────────────

  /**
   * Fetches every distinct recipe tag via the {@code distinct_tags} SQL
   * view. Used to populate the Trie for tag autocomplete in search.
   */
  @GET("distinct_tags")
  Call<List<TagDto>> getAllTags(
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

    @GET("follows")
    Call<List<FollowDto>> getFollowers(
            @Query("following_id") String followingIdFilter,
            @Query("select") String select
    );


    @GET("follows")
    Call<List<FollowDto>> getFollowing(
            @Query("follower_id") String followerIdFilter,
            @Query("select") String select
    );

    /**
     * Checks whether a specific follower→following row exists. Pass
     * filters in PostgREST {@code eq.<uuid>} form. Empty list = not
     * following; one row = following.
     */
    @GET("follows")
    Call<List<FollowDto>> checkFollow(
            @Query("follower_id") String followerIdFilter,
            @Query("following_id") String followingIdFilter
    );

    @POST("follows")
    Call<List<FollowDto>> followUser(@Body CreateFollowRequest request);


    @DELETE("follows")
    Call<Void> unfollowUser(
            @Query("follower_id") String followerIdFilter,
            @Query("following_id") String followingIdFilter
    );

    @GET("saved_recipes")
    Call<List<SavedRecipeDto>> getSavedRecipes(
            @Query("user_id") String userIdFilter,
            @Query("select") String select,
            @Query("order") String order
    );

    /** Inserts a row into {@code saved_recipes}. */
    @POST("saved_recipes")
    Call<List<SavedRecipeDto>> createSavedRecipe(
        @Body CreateSavedRecipeRequest request
    );

    /** Deletes a row from {@code saved_recipes} by user and recipe filters. */
    @DELETE("saved_recipes")
    Call<Void> deleteSavedRecipe(
        @Query("user_id") String userIdFilter,
        @Query("recipe_id") String recipeIdFilter
    );

  // ── Profiles (read) ──────────────────────────────────────────────────

  /** Fetches one or more profiles by ID (supports {@code eq.<id>} or {@code in.(id1,id2,...)}). */
  @GET("profiles")
  Call<List<UserDto>> getProfiles(
      @Query("id") String idFilter,
      @Query("select") String select
  );

  /** Searches profiles by username prefix using PostgREST {@code ilike} filter. */
  @GET("profiles")
  Call<List<UserDto>> searchProfiles(
      @Query("username") String usernameFilter,
      @Query("select") String select,
      @Query("order") String order
  );

  // ── Follow relationship check ─────────────────────────────────────────

  /** Returns the follow row if the relationship exists, empty list otherwise. */
  @GET("follows")
  Call<List<FollowDto>> checkFollow(
      @Query("follower_id") String followerIdFilter,
      @Query("following_id") String followingIdFilter,
      @Query("select") String select
  );

}
