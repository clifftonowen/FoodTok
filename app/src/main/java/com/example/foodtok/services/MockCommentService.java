package com.example.foodtok.services;

import com.example.foodtok.models.Comment;

import java.util.ArrayList;
import java.util.List;

/** In-memory mock implementation of {@link ICommentService} for offline testing. */
public class MockCommentService implements ICommentService {

  @Override
  public void getComments(String recipeId, CommentListCallback callback) {
    callback.onSuccess(buildMockComments());
  }

  @Override
  public void postComment(String recipeId, String text,
      CommentCallback callback) {
    Comment comment = new Comment(
        "local_" + System.currentTimeMillis(),
        "You", null, text, System.currentTimeMillis());
    callback.onSuccess(comment);
  }

  @Override
  public void getCommentCount(String recipeId, IntCallback callback) {
    callback.onResult(buildMockComments().size());
  }

  private List<Comment> buildMockComments() {
    List<Comment> list = new ArrayList<>();
    long now = System.currentTimeMillis();
    list.add(new Comment("c1", "foodie_alice", null,
        "This looks absolutely delicious! Can't wait to try it.",
        now - 3600_000L));
    list.add(new Comment("c2", "chef_marco", null,
        "Pro tip: add a pinch of smoked paprika for extra depth.",
        now - 7200_000L));
    list.add(new Comment("c3", "healthy_eats", null,
        "Does this work with oat milk instead of regular milk?",
        now - 86400_000L));
    list.add(new Comment("c4", "ramen_lover99", null,
        "Made this last night and my family loved it!",
        now - 2 * 86400_000L));
    return list;
  }
}
