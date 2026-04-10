package com.example.foodtok.services;

import android.content.Context;
import android.net.Uri;

import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.dto.RecipeDto;
import com.example.foodtok.models.dto.UploadRecipeRequest;
import com.example.foodtok.util.ApiClient;
import com.example.foodtok.util.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Real {@link IRecipeService} implementation backed by Supabase PostgREST. */
public class SupabaseRecipeService implements IRecipeService {

  private static final String RECIPE_SELECT =
      "*,recipe_ingredients(quantity,is_optional,"
      + "ingredients(id,name,calories_per_100g)),"
      + "profiles!author_id(username)";

  private final SupabaseApi api;
  private final SupabaseStorageApi storageApi;

  public SupabaseRecipeService() {
    this.api = ApiClient.getRestClient().create(SupabaseApi.class);
    this.storageApi = ApiClient.getStorageClient()
        .create(SupabaseStorageApi.class);
  }

  @Override
  public void getFeedRecipes(int page, int pageSize,
      RecipeListCallback callback) {
    int from = page * pageSize;
    int to = from + pageSize - 1;
    String range = from + "-" + to;

    api.getRecipes(RECIPE_SELECT, "created_at.desc", range)
        .enqueue(new Callback<List<RecipeDto>>() {
          @Override
          public void onResponse(Call<List<RecipeDto>> call,
              Response<List<RecipeDto>> response) {
            if (response.isSuccessful() && response.body() != null) {
              List<Recipe> recipes = new ArrayList<>();
              for (RecipeDto dto : response.body()) {
                recipes.add(dto.toDomain());
              }
              callback.onSuccess(recipes);
            } else {
              callback.onError("Failed to load recipes: "
                  + response.code());
            }
          }

          @Override
          public void onFailure(Call<List<RecipeDto>> call, Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  @Override
  public void getRecipeById(String recipeId, RecipeCallback callback) {
    api.getRecipeById("eq." + recipeId, RECIPE_SELECT)
        .enqueue(new Callback<List<RecipeDto>>() {
          @Override
          public void onResponse(Call<List<RecipeDto>> call,
              Response<List<RecipeDto>> response) {
            if (response.isSuccessful() && response.body() != null
                && !response.body().isEmpty()) {
              callback.onSuccess(response.body().get(0).toDomain());
            } else {
              callback.onError("Recipe not found");
            }
          }

          @Override
          public void onFailure(Call<List<RecipeDto>> call, Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  @Override
  public void uploadRecipe(Context context, Uri videoUri, String title,
      String description, String[] tags, int prepTimeMinutes,
      int cookTimeMinutes, double estimatedCalories,
      RecipeCallback callback) {
    String userId = AuthManager.getInstance().getCurrentUser().getId();
    String fileId = UUID.randomUUID().toString();
    String storagePath = userId + "/" + fileId + ".mp4";

    // Step 1: Read the video bytes from the content Uri
    byte[] videoBytes;
    try {
      videoBytes = readBytes(context, videoUri);
    } catch (IOException e) {
      callback.onError("Failed to read video file: " + e.getMessage());
      return;
    }

    RequestBody body = RequestBody.create(
        MediaType.parse("video/mp4"), videoBytes);

    // Step 2: Upload video to Supabase Storage
    storageApi.uploadFile("videos", storagePath, "video/mp4", body)
        .enqueue(new Callback<ResponseBody>() {
          @Override
          public void onResponse(Call<ResponseBody> call,
              Response<ResponseBody> response) {
            if (response.isSuccessful()) {
              // Step 3: Build the public URL and create the recipe row
              String videoUrl = Constants.STORAGE_BASE_URL
                  + "object/public/videos/" + storagePath;
              createRecipeRow(userId, title, description, videoUrl,
                  tags, prepTimeMinutes, cookTimeMinutes,
                  estimatedCalories, callback);
            } else {
              callback.onError("Video upload failed: "
                  + response.code());
            }
          }

          @Override
          public void onFailure(Call<ResponseBody> call, Throwable t) {
            callback.onError("Video upload error: " + t.getMessage());
          }
        });
  }

  /**
   * Creates the recipe row in PostgREST after the video has been uploaded.
   */
  private void createRecipeRow(String authorId, String title,
      String description, String videoUrl, String[] tags,
      int prepTimeMinutes, int cookTimeMinutes,
      double estimatedCalories, RecipeCallback callback) {
    UploadRecipeRequest request = new UploadRecipeRequest();
    request.authorId = authorId;
    request.title = title;
    request.description = description;
    request.videoUrl = videoUrl;
    request.tags = tags;
    request.prepTimeMinutes = prepTimeMinutes;
    request.cookTimeMinutes = cookTimeMinutes;
    request.estimatedCalories = estimatedCalories;

    api.createRecipe(request)
        .enqueue(new Callback<List<RecipeDto>>() {
          @Override
          public void onResponse(Call<List<RecipeDto>> call,
              Response<List<RecipeDto>> response) {
            if (response.isSuccessful() && response.body() != null
                && !response.body().isEmpty()) {
              callback.onSuccess(
                  response.body().get(0).toDomain());
            } else {
              callback.onError("Failed to create recipe: "
                  + response.code());
            }
          }

          @Override
          public void onFailure(Call<List<RecipeDto>> call,
              Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  /** Reads all bytes from a content:// Uri. */
  private byte[] readBytes(Context context, Uri uri)
      throws IOException {
    try (InputStream is = context.getContentResolver()
        .openInputStream(uri)) {
      if (is == null) {
        throw new IOException("Cannot open input stream for " + uri);
      }
      java.io.ByteArrayOutputStream buffer =
          new java.io.ByteArrayOutputStream();
      byte[] chunk = new byte[8192];
      int bytesRead;
      while ((bytesRead = is.read(chunk)) != -1) {
        buffer.write(chunk, 0, bytesRead);
      }
      return buffer.toByteArray();
    }
  }
}
