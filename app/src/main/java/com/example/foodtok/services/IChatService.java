package com.example.foodtok.services;

import com.example.foodtok.models.ChatMessage;
import com.example.foodtok.models.Recipe;

import java.util.List;

/**
 * Contract for recipe chat functionality.
 * Implementations: GeminiChatService (real), MockChatService (testing).
 *
 * OOP: Interface for loose coupling — UI code depends on this contract,
 * not a specific implementation. Enables swapping via ChatServiceProvider.
 */
public interface IChatService {

    /**
     * Sends a user message in the context of a specific recipe.
     * The service maintains conversation history per recipe.
     *
     * @param recipeId    identifies which recipe's conversation to use
     * @param recipe      the full Recipe object (used to build context prompt)
     * @param userMessage the user's question
     * @param callback    receives the model's response or an error
     */
    void sendMessage(String recipeId, Recipe recipe, String userMessage,
                     ChatCallback callback);

    /**
     * Returns the conversation history for a given recipe.
     * Used to populate the chat RecyclerView when the page is shown.
     */
    List<ChatMessage> getHistory(String recipeId);

    /**
     * Clears conversation history for a given recipe.
     */
    void clearHistory(String recipeId);
}
