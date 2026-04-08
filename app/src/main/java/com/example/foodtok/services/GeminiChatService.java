package com.example.foodtok.services;

import androidx.annotation.NonNull;

import com.example.foodtok.models.ChatMessage;
import com.example.foodtok.models.Ingredient;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.dto.GeminiRequest;
import com.example.foodtok.models.dto.GeminiResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Real implementation of IChatService using Gemini REST API.
 *
 * Maintains per-recipe conversation history in memory (session-scoped).
 * Builds a system prompt from the Recipe's ingredients, tags, and metadata
 * so the chatbot can answer context-aware questions.
 *
 * OOP: Implements IChatService interface (polymorphism).
 * Encapsulation: API key and conversation state are private.
 */
public class GeminiChatService implements IChatService {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";
    private static final int MAX_HISTORY_SIZE = 6;

    private final String apiKey;
    private final GeminiApi geminiApi;
    private final Map<String, List<ChatMessage>> conversationHistory;

    public GeminiChatService(String apiKey) {
        this.apiKey = apiKey;
        this.conversationHistory = new HashMap<>();

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.NONE);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.geminiApi = retrofit.create(GeminiApi.class);
    }

    @Override
    public void sendMessage(String recipeId, Recipe recipe, String userMessage,
                            ChatCallback callback) {
        List<ChatMessage> history = getOrCreateHistory(recipeId);

        // Note: the user message is already added to history by the UI layer
        // (adapter.addMessage shares the same list reference). Do NOT add it again.
        trimHistory(history);

        // Build the API request
        String systemPrompt = buildSystemPrompt(recipe);
        List<GeminiRequest.Content> contents = buildContents(history);
        GeminiRequest request = GeminiRequest.create(systemPrompt, contents);

        // Make async API call
        geminiApi.generateContent(apiKey, request).enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeminiResponse> call,
                                   @NonNull Response<GeminiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String text = response.body().getText();
                    if (text != null) {
                        ChatMessage botMsg = new ChatMessage("model", text);
                        callback.onResponse(botMsg);
                    } else {
                        callback.onError("Empty response from Gemini");
                    }
                } else if (response.code() == 429) {
                    callback.onError("Too many requests — please wait a moment before asking again.");
                } else {
                    callback.onError("Gemini API error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<GeminiResponse> call,
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

    private void trimHistory(List<ChatMessage> history) {
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }
    }

    /**
     * Builds a system prompt with full recipe context so the chatbot
     * can answer questions about ingredients, substitutions, equipment, etc.
     */
    private String buildSystemPrompt(Recipe recipe) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a helpful cooking assistant for the recipe \"")
          .append(recipe.getTitle()).append("\"");

        if (recipe.getAuthorName() != null && !recipe.getAuthorName().isEmpty()) {
            sb.append(" by ").append(recipe.getAuthorName());
        }
        sb.append(".\n\n");

        // Ingredients with calorie and allergen info
        List<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients != null && !ingredients.isEmpty()) {
            sb.append("Ingredients:\n");
            for (Ingredient ing : ingredients) {
                sb.append("- ").append(ing.getName());
                sb.append(" (").append((int) ing.getCalories()).append(" kcal)");
                sb.append(" [ALLERGEN]");
                sb.append("\n");
            }
            sb.append("\n");
        }

        // Timing and nutrition
        if (recipe.getPrepTimeMinutes() > 0 || recipe.getCookTimeMinutes() > 0) {
            sb.append("Prep: ").append(recipe.getPrepTimeMinutes()).append(" min | ");
            sb.append("Cook: ").append(recipe.getCookTimeMinutes()).append(" min");
            if (recipe.getEstimatedCalories() > 0) {
                sb.append(" | ~").append((int) recipe.getEstimatedCalories()).append(" kcal total");
            }
            sb.append("\n\n");
        }

        // Tags
        List<String> tags = recipe.getTags();
        if (tags != null && !tags.isEmpty()) {
            sb.append("Tags: ").append(String.join(", ", tags)).append("\n\n");
        }

        sb.append("Help the user with ingredient substitutions, cooking techniques, ")
          .append("equipment needed, nutritional information, dietary modifications, ")
          .append("and step-by-step guidance. Keep answers concise and practical. ")
          .append("If asked about something unrelated to cooking or this recipe, ")
          .append("politely redirect the conversation.");

        return sb.toString();
    }

    /**
     * Converts ChatMessage history to Gemini API Content objects.
     */
    private List<GeminiRequest.Content> buildContents(List<ChatMessage> history) {
        List<GeminiRequest.Content> contents = new ArrayList<>();
        for (ChatMessage msg : history) {
            if (msg.isUser()) {
                contents.add(GeminiRequest.Content.ofUser(msg.getText()));
            } else {
                contents.add(GeminiRequest.Content.ofModel(msg.getText()));
            }
        }
        return contents;
    }
}
