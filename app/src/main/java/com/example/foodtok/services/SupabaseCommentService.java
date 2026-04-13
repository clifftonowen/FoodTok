package com.example.foodtok.services;

import com.example.foodtok.models.Comment;
import com.example.foodtok.models.dto.CommentDto;
import com.example.foodtok.models.dto.CreateCommentRequest;
import com.example.foodtok.util.ApiClient;
import com.example.foodtok.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Real {@link ICommentService} implementation backed by Supabase PostgREST. */
public class SupabaseCommentService implements ICommentService {

  private static final String COMMENT_SELECT =
      "*,profiles(username)";

  private final SupabaseApi api;

  public SupabaseCommentService() {
    this.api = ApiClient.getRestClient().create(SupabaseApi.class);
  }

  @Override
  public void getComments(String recipeId, CommentListCallback callback) {
    api.getComments("eq." + recipeId, COMMENT_SELECT, "created_at.desc")
        .enqueue(new Callback<List<CommentDto>>() {
          @Override
          public void onResponse(Call<List<CommentDto>> call,
              Response<List<CommentDto>> response) {
            if (response.isSuccessful() && response.body() != null) {
              List<Comment> comments = new ArrayList<>();
              for (CommentDto dto : response.body()) {
                comments.add(dto.toDomain());
              }
              callback.onSuccess(comments);
            } else {
              callback.onError("Failed to load comments: "
                  + response.code());
            }
          }

          @Override
          public void onFailure(Call<List<CommentDto>> call, Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  @Override
  public void getCommentCount(String recipeId, IntCallback callback) {
    // Fetch only the id column to minimise payload — we just need the count.
    api.getComments("eq." + recipeId, "id", null)
        .enqueue(new Callback<List<CommentDto>>() {
          @Override
          public void onResponse(Call<List<CommentDto>> call,
              Response<List<CommentDto>> response) {
            if (response.isSuccessful() && response.body() != null) {
              callback.onResult(response.body().size());
            } else {
              callback.onError("Failed to fetch comment count: " + response.code());
            }
          }

          @Override
          public void onFailure(Call<List<CommentDto>> call, Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  @Override
  public void postComment(String recipeId, String text,
      CommentCallback callback) {
    String userId = SessionManager.getInstance().getUserId();
    if (userId == null) {
      callback.onError("Please log in first");
      return;
    }

    CreateCommentRequest request =
        new CreateCommentRequest(userId, recipeId, text);

    api.createComment(request)
        .enqueue(new Callback<List<CommentDto>>() {
          @Override
          public void onResponse(Call<List<CommentDto>> call,
              Response<List<CommentDto>> response) {
            if (response.isSuccessful() && response.body() != null
                && !response.body().isEmpty()) {
              callback.onSuccess(response.body().get(0).toDomain());
            } else {
              callback.onError("Failed to post comment: "
                  + response.code());
            }
          }

          @Override
          public void onFailure(Call<List<CommentDto>> call, Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }
}
