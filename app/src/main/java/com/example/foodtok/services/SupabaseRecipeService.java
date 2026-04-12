package com.example.foodtok.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.models.IngredientInput;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.dto.CreateIngredientRequest;
import com.example.foodtok.models.dto.CreateRecipeIngredientRequest;
import com.example.foodtok.models.dto.IngredientDto;
import com.example.foodtok.models.dto.RecipeDto;
import com.example.foodtok.models.dto.UploadRecipeRequest;
import com.example.foodtok.util.ApiClient;
import com.example.foodtok.util.Constants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Real {@link IRecipeService} implementation backed by Supabase PostgREST. */
public class SupabaseRecipeService implements IRecipeService {

  private static final String TAG = "SupabaseRecipeService";

  private static final String RECIPE_SELECT =
      "*,recipe_ingredients(quantity,is_optional,"
      + "ingredients(id,name,calories_per_100g)),"
      + "profiles!author_id(id,username,avatar_url)";

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
      String description, String[] tags, List<IngredientInput> ingredients,
      int prepTimeMinutes, int cookTimeMinutes, double estimatedCalories,
      RecipeCallback callback) {
    String userId = AuthManager.getInstance().getCurrentUser().getId();
    String fileId = UUID.randomUUID().toString();
    String videoStoragePath = userId + "/" + fileId + ".mp4";
    String thumbnailStoragePath = userId + "/" + fileId + ".jpg";

    // Step 1: Extract a thumbnail from the video. If extraction fails,
    // we still proceed with the upload — the grid has a client-side
    // fallback that decodes the first frame on demand.
    final byte[] thumbnailBytes = extractThumbnailJpeg(context, videoUri);

    // Step 2: Build a streaming request body over the content Uri —
    // never materialize the entire video into a byte[] (phones easily
    // run into OOM for >50MB videos).
    RequestBody videoBody = streamingBody(
        context.getApplicationContext(), videoUri, "video/mp4");

    // Step 3: Upload video to Supabase Storage
    storageApi.uploadFile("videos", videoStoragePath, "video/mp4", videoBody)
        .enqueue(new Callback<ResponseBody>() {
          @Override
          public void onResponse(Call<ResponseBody> call,
              Response<ResponseBody> response) {
            if (!response.isSuccessful()) {
              callback.onError("Video upload failed: "
                  + response.code());
              return;
            }
            String videoUrl = Constants.STORAGE_BASE_URL
                + "object/public/videos/" + videoStoragePath;

            // Step 4: Upload thumbnail (best-effort, non-blocking for
            // the recipe row creation on failure).
            final List<IngredientInput> ingredientsRef = ingredients;
            if (thumbnailBytes == null) {
              createRecipeRow(userId, title, description, videoUrl, null,
                  tags, prepTimeMinutes, cookTimeMinutes,
                  estimatedCalories, withIngredients(ingredientsRef, callback));
              return;
            }
            uploadThumbnail(thumbnailBytes, thumbnailStoragePath, thumbUrl ->
                createRecipeRow(userId, title, description, videoUrl, thumbUrl,
                    tags, prepTimeMinutes, cookTimeMinutes,
                    estimatedCalories, withIngredients(ingredientsRef, callback)));
          }

          @Override
          public void onFailure(Call<ResponseBody> call, Throwable t) {
            callback.onError("Video upload error: " + t.getMessage());
          }
        });
  }

  /** Simple single-arg callback for the thumbnail upload step. */
  private interface ThumbnailUploadCallback {
    void onDone(String thumbnailUrl);
  }

  /**
   * Uploads a JPEG thumbnail to the "thumbnails" bucket. On failure the
   * callback is still invoked with {@code null} so recipe creation can
   * proceed — a missing thumbnail is non-fatal.
   */
  private void uploadThumbnail(byte[] jpegBytes, String storagePath,
      ThumbnailUploadCallback callback) {
    RequestBody body = RequestBody.create(
        MediaType.parse("image/jpeg"), jpegBytes);
    storageApi.uploadFile("thumbnails", storagePath, "image/jpeg", body)
        .enqueue(new Callback<ResponseBody>() {
          @Override
          public void onResponse(Call<ResponseBody> call,
              Response<ResponseBody> response) {
            if (response.isSuccessful()) {
              callback.onDone(Constants.STORAGE_BASE_URL
                  + "object/public/thumbnails/" + storagePath);
            } else {
              callback.onDone(null);
            }
          }

          @Override
          public void onFailure(Call<ResponseBody> call, Throwable t) {
            callback.onDone(null);
          }
        });
  }

  /**
   * Extracts a frame from the video at the 1-second mark, scales it to
   * a reasonable thumbnail size, and encodes as JPEG. Returns null if
   * extraction fails for any reason.
   */
  private byte[] extractThumbnailJpeg(Context context, Uri videoUri) {
    final int targetWidth = 720;
    final int targetHeight = 1080;
    final long frameTimeUs = 1_000_000L;
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    Bitmap frame = null;
    try {
      retriever.setDataSource(context, videoUri);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        frame = retriever.getScaledFrameAtTime(
            frameTimeUs,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            targetWidth,
            targetHeight);
      }
      if (frame == null) {
        frame = retriever.getFrameAtTime(
            frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
      }
      if (frame == null) {
        return null;
      }
      if (frame.getWidth() > targetWidth * 2) {
        Bitmap scaled = Bitmap.createScaledBitmap(
            frame, targetWidth, targetHeight, true);
        if (scaled != frame) {
          frame.recycle();
          frame = scaled;
        }
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      frame.compress(Bitmap.CompressFormat.JPEG, 80, out);
      return out.toByteArray();
    } catch (RuntimeException e) {
      return null;
    } finally {
      if (frame != null) {
        frame.recycle();
      }
      try {
        retriever.release();
      } catch (IOException | RuntimeException ignored) {
      }
    }
  }

  /**
   * Creates the recipe row in PostgREST after the video has been uploaded.
   */
  private void createRecipeRow(String authorId, String title,
      String description, String videoUrl, String thumbnailUrl,
      String[] tags, int prepTimeMinutes, int cookTimeMinutes,
      double estimatedCalories, RecipeCallback callback) {
    UploadRecipeRequest request = new UploadRecipeRequest();
    request.authorId = authorId;
    request.title = title;
    request.description = description;
    request.videoUrl = videoUrl;
    request.thumbnailUrl = thumbnailUrl;
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

  /**
   * Wraps the caller's {@link RecipeCallback} so that on a successful recipe
   * row creation we fan out a series of ingredient upserts + a batched
   * recipe_ingredients insert before forwarding {@code onSuccess}. If
   * {@code ingredients} is null or empty the wrapper is a no-op and the
   * original callback is forwarded directly.
   *
   * <p>Persistence flow:
   * <ol>
   *   <li>For each input, upsert the ingredient by name (merge-duplicates)
   *       to obtain its UUID.</li>
   *   <li>Once all ids are resolved, POST the full list of join rows in
   *       one request.</li>
   * </ol>
   * Errors in ingredient persistence are logged via the callback as
   * warnings but the recipe itself is still considered posted — the recipe
   * row already exists and the video is uploaded.
   */
  private RecipeCallback withIngredients(
      List<IngredientInput> ingredients, RecipeCallback original) {
    if (ingredients == null || ingredients.isEmpty()) {
      return original;
    }
    return new RecipeCallback() {
      @Override
      public void onSuccess(Recipe recipe) {
        persistIngredients(recipe, ingredients, original);
      }

      @Override
      public void onError(String message) {
        original.onError(message);
      }
    };
  }

  /**
   * Resolves each ingredient name to an id via lookup-then-insert (avoids
   * needing UPDATE RLS rights that a PostgREST upsert would require). For
   * every input:
   * <ol>
   *   <li>GET {@code /ingredients?name=eq.<name>&select=id}</li>
   *   <li>If the row exists, use its id.</li>
   *   <li>Otherwise POST a new ingredient row and use the returned id.</li>
   * </ol>
   * Once every id is resolved, all join rows are batch-inserted in one POST.
   * Ingredient persistence failures are logged and the recipe is still
   * reported as successfully posted — the recipe row and video already
   * exist, they just have no ingredients attached.
   */
  private void persistIngredients(Recipe recipe,
      List<IngredientInput> inputs, RecipeCallback callback) {
    final String[] ingredientIds = new String[inputs.size()];
    final int[] resolvedCount = {0};
    final boolean[] failed = {false};

    for (int i = 0; i < inputs.size(); i++) {
      resolveIngredientId(recipe, inputs, i, ingredientIds,
          resolvedCount, failed, callback);
    }
  }

  /** Resolves a single ingredient name to an id (GET then POST if missing). */
  private void resolveIngredientId(Recipe recipe, List<IngredientInput> inputs,
      int index, String[] ingredientIds, int[] resolvedCount,
      boolean[] failed, RecipeCallback callback) {
    final IngredientInput input = inputs.get(index);
    final String name = input.getName().toLowerCase();

    api.getIngredientByName("eq." + name, "id")
        .enqueue(new Callback<List<IngredientDto>>() {
          @Override
          public void onResponse(Call<List<IngredientDto>> call,
              Response<List<IngredientDto>> response) {
            if (failed[0]) {
              return;
            }
            if (!response.isSuccessful()) {
              fail("Ingredient lookup failed for '" + name + "': HTTP "
                  + response.code());
              return;
            }
            List<IngredientDto> body = response.body();
            if (body != null && !body.isEmpty()) {
              ingredientIds[index] = body.get(0).id;
              markResolved();
            } else {
              createAndResolve();
            }
          }

          @Override
          public void onFailure(Call<List<IngredientDto>> call, Throwable t) {
            fail("Ingredient lookup network error for '" + name + "': "
                + t.getMessage());
          }

          /** POSTs a new ingredient row and stores its id on success. */
          private void createAndResolve() {
            api.createIngredient(new CreateIngredientRequest(name))
                .enqueue(new Callback<List<IngredientDto>>() {
                  @Override
                  public void onResponse(Call<List<IngredientDto>> call,
                      Response<List<IngredientDto>> response) {
                    if (failed[0]) {
                      return;
                    }
                    if (response.isSuccessful() && response.body() != null
                        && !response.body().isEmpty()) {
                      ingredientIds[index] = response.body().get(0).id;
                      markResolved();
                    } else {
                      fail("Ingredient insert failed for '" + name
                          + "': HTTP " + response.code());
                    }
                  }

                  @Override
                  public void onFailure(Call<List<IngredientDto>> call,
                      Throwable t) {
                    fail("Ingredient insert network error for '" + name
                        + "': " + t.getMessage());
                  }
                });
          }

          private void markResolved() {
            resolvedCount[0]++;
            if (resolvedCount[0] == inputs.size()) {
              insertRecipeIngredientRows(recipe, inputs, ingredientIds,
                  callback);
            }
          }

          private void fail(String message) {
            if (failed[0]) {
              return;
            }
            Log.w(TAG, message);
            failed[0] = true;
            callback.onSuccess(recipe);
          }
        });
  }

  /** Batches all join rows into a single POST to {@code recipe_ingredients}. */
  private void insertRecipeIngredientRows(Recipe recipe,
      List<IngredientInput> inputs, String[] ingredientIds,
      RecipeCallback callback) {
    List<CreateRecipeIngredientRequest> rows = new ArrayList<>();
    for (int i = 0; i < inputs.size(); i++) {
      String quantity = inputs.get(i).getQuantity();
      if (quantity == null || quantity.trim().isEmpty()) {
        quantity = "1";
      }
      rows.add(new CreateRecipeIngredientRequest(
          recipe.getId(), ingredientIds[i], quantity, false));
    }
    api.createRecipeIngredients(rows).enqueue(new Callback<Void>() {
      @Override
      public void onResponse(Call<Void> call, Response<Void> response) {
        if (!response.isSuccessful()) {
          Log.w(TAG, "recipe_ingredients batch insert failed: HTTP "
              + response.code());
        } else {
          Log.d(TAG, "recipe_ingredients inserted: " + rows.size()
              + " rows for recipe " + recipe.getId());
        }
        callback.onSuccess(recipe);
      }

      @Override
      public void onFailure(Call<Void> call, Throwable t) {
        Log.w(TAG, "recipe_ingredients batch insert network error", t);
        callback.onSuccess(recipe);
      }
    });
  }

  @Override
  public void searchByIngredients(Set<String> searchTokens,
      RecipeListCallback callback) {
    // Tokens may be tag names OR ingredient names — we rank by how many
    // distinct tokens each recipe matches (logical OR). Fetch all recipes
    // then rank client-side.
    api.getRecipes(RECIPE_SELECT, "created_at.desc", "0-99")
        .enqueue(new Callback<List<RecipeDto>>() {
          @Override
          public void onResponse(Call<List<RecipeDto>> call,
              Response<List<RecipeDto>> response) {
            if (response.isSuccessful() && response.body() != null) {
              List<Recipe> matched = new ArrayList<>();
              List<Recipe> all = new ArrayList<>();
              for (RecipeDto dto : response.body()) {
                Recipe recipe = dto.toDomain();
                all.add(recipe);
                if (recipe.countMatchingTokens(searchTokens) > 0) {
                  matched.add(recipe);
                }
              }
              if (matched.isEmpty()) {
                // No exact matches — return all recipes shuffled
                // as "suggested" results.
                Collections.shuffle(all);
                callback.onSuccess(all);
              } else {
                // Sort by distinct token match count descending
                Collections.sort(matched, (a, b) ->
                    Integer.compare(
                        b.countMatchingTokens(searchTokens),
                        a.countMatchingTokens(searchTokens)));
                callback.onSuccess(matched);
              }
            } else {
              callback.onError("Search failed: " + response.code());
            }
          }

          @Override
          public void onFailure(Call<List<RecipeDto>> call,
              Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  /**
   * Builds an OkHttp {@link RequestBody} that streams directly from a
   * content:// Uri via Okio, without ever buffering the whole file into
   * a byte array. Uses {@code AssetFileDescriptor} to report the exact
   * content length so Supabase Storage receives a proper
   * {@code Content-Length} header instead of chunked transfer.
   */
  private RequestBody streamingBody(Context context, Uri uri,
      String mediaType) {
    final MediaType parsedType = MediaType.parse(mediaType);
    return new RequestBody() {
      @Override
      public MediaType contentType() {
        return parsedType;
      }

      @Override
      public long contentLength() {
        try (android.content.res.AssetFileDescriptor afd =
            context.getContentResolver().openAssetFileDescriptor(uri, "r")) {
          if (afd == null) {
            return -1;
          }
          long len = afd.getLength();
          return len == android.content.res.AssetFileDescriptor
              .UNKNOWN_LENGTH ? -1 : len;
        } catch (IOException e) {
          return -1;
        }
      }

      @Override
      public void writeTo(BufferedSink sink) throws IOException {
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) {
          throw new IOException("Cannot open input stream for " + uri);
        }
        try (Source source = Okio.source(in)) {
          sink.writeAll(source);
        }
      }
    };
  }
}
