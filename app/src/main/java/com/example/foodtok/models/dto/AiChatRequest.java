package com.example.foodtok.models.dto;

import com.example.foodtok.models.ChatMessage;
import com.example.foodtok.models.Recipe;

import java.util.ArrayList;
import java.util.List;

/** Request body for the {@code gemini-chat} Supabase Edge Function. */
public class AiChatRequest {

  private final AiRecipeContext recipe;
  private final List<Turn> history;

  private AiChatRequest(AiRecipeContext recipe, List<Turn> history) {
    this.recipe = recipe;
    this.history = history;
  }

  /**
   * Builds a chat request from a recipe and the caller's conversation history.
   *
   * <p>The history is expected to <em>already contain</em> the user's latest message —
   * the UI layer appends it before the service is called, and the adapter shares the same
   * list reference. Do not append it again here or the model will see it twice.
   *
   * @param recipe the recipe the conversation is about
   * @param history the conversation so far, oldest first, latest user turn included
   * @return a serialisable request body
   */
  public static AiChatRequest create(Recipe recipe, List<ChatMessage> history) {
    List<Turn> turns = new ArrayList<>();
    for (ChatMessage message : history) {
      turns.add(new Turn(message.isUser() ? "user" : "model", message.getText()));
    }
    return new AiChatRequest(AiRecipeContext.from(recipe), turns);
  }

  /** A single conversation turn in the format the Edge Function expects. */
  public static class Turn {

    private final String role;
    private final String text;

    Turn(String role, String text) {
      this.role = role;
      this.text = text;
    }

    public String getRole() {
      return role;
    }

    public String getText() {
      return text;
    }
  }
}
