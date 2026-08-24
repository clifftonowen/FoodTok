package com.example.foodtok.services;

import androidx.annotation.NonNull;

import com.example.foodtok.models.ChatMessage;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.dto.AiChatRequest;
import com.example.foodtok.models.dto.AiTextResponse;
import com.example.foodtok.util.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Real implementation of {@link IChatService}, backed by the {@code gemini-chat} Supabase
 * Edge Function.
 *
 * <p>Replaces the former on-device Gemini client. The Gemini API key now lives as a
 * Supabase secret and never reaches the APK; the system prompt is built server-side so the
 * request body stays a typed recipe object rather than free-form model input.
 *
 * <p>Conversation memory deliberately stays here on the device. Edge Functions are
 * stateless, and the UI shares this service's live history list with the chat adapter, so
 * moving history server-side would break incremental rendering for no security gain.
 *
 * <p>OOP: implements IChatService (polymorphism). Encapsulation: conversation state is
 * private and only reachable through getHistory/clearHistory.
 */
public class SupabaseChatService implements IChatService {

  /** How many of the most recent messages are sent to the model as context. */
  private static final int MAX_CONTEXT_MESSAGES = 6;

  private final AiProxyApi api;
  private final Map<String, List<ChatMessage>> conversationHistory;

  public SupabaseChatService() {
    this.conversationHistory = new HashMap<>();
    this.api = ApiClient.getFunctionsClient().create(AiProxyApi.class);
  }

  @Override
  public void sendMessage(String recipeId, Recipe recipe, String userMessage,
              ChatCallback callback) {
    List<ChatMessage> history = getOrCreateHistory(recipeId);

    // Note: the user message is already added to history by the UI layer
    // (adapter.addMessage shares the same list reference). Do NOT add it again.
    //
    // That sharing is also why we must not trim `history` in place: it is the backing
    // list of a ChatMessageAdapter, and removing from it without notifying RecyclerView
    // desyncs the item count and crashes the next layout pass. Bound what we *send*
    // instead, and leave what is *displayed* alone.
    AiChatRequest request =
        AiChatRequest.create(recipe, contextWindow(history, MAX_CONTEXT_MESSAGES));

    api.chat(request).enqueue(new Callback<AiTextResponse>() {
      @Override
      public void onResponse(@NonNull Call<AiTextResponse> call,
                                   @NonNull Response<AiTextResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
          String text = response.body().getText();
          if (text != null) {
            ChatMessage botMsg = new ChatMessage("model", text);
            callback.onResponse(botMsg);
          } else {
            callback.onError("Empty response from the AI service");
          }
        } else if (response.code() == 429) {
          callback.onError("Too many requests — please wait a moment before asking again.");
        } else {
          callback.onError("AI service error: " + response.code());
        }
      }

      @Override
      public void onFailure(@NonNull Call<AiTextResponse> call,
                 @NonNull Throwable t) {
        callback.onError("Network error: " + t.getMessage());
      }
    });
  }

  @Override
  public List<ChatMessage> getHistory(String recipeId) {
    return getOrCreateHistory(recipeId);
  }

  @Override
  public void clearHistory(String recipeId) {
    conversationHistory.remove(recipeId);
  }

  // --- Private helpers ---

  private List<ChatMessage> getOrCreateHistory(String recipeId) {
    if (!conversationHistory.containsKey(recipeId)) {
      conversationHistory.put(recipeId, new ArrayList<>());
    }
    return conversationHistory.get(recipeId);
  }

  /**
   * Returns the most recent {@code max} messages to send to the model.
   *
   * <p>Never mutates {@code history}. The caller's list is shared by reference with
   * {@code ChatMessageAdapter} as its RecyclerView backing store, so trimming it in place
   * would change the adapter's item count with no notification and throw
   * {@code IndexOutOfBoundsException: Inconsistency detected} on the next layout pass.
   *
   * <p>Returns the original list unchanged when it is already within the limit, so short
   * conversations avoid a pointless copy.
   *
   * @param history the full conversation, oldest first
   * @param max the maximum number of messages to include
   * @return the last {@code max} messages, or {@code history} itself if it is shorter
   */
  static List<ChatMessage> contextWindow(List<ChatMessage> history, int max) {
    if (history.size() <= max) {
      return history;
    }
    return new ArrayList<>(history.subList(history.size() - max, history.size()));
  }
}
