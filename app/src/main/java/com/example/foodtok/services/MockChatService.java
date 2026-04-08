package com.example.foodtok.services;

import com.example.foodtok.models.ChatMessage;
import com.example.foodtok.models.Recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock implementation of IChatService for testing without a Gemini API key.
 * Returns canned responses based on the recipe context.
 *
 * Follows the same pattern as MockInteractionService and MockAuthService.
 */
public class MockChatService implements IChatService {

    private final Map<String, List<ChatMessage>> conversationHistory = new HashMap<>();

    @Override
    public void sendMessage(String recipeId, Recipe recipe, String userMessage,
                            ChatCallback callback) {
        List<ChatMessage> history = getOrCreateHistory(recipeId);

        // Note: user message already added by UI layer (adapter shares same list reference)
        // Generate a canned response based on the recipe
        String response = generateMockResponse(recipe, userMessage);
        ChatMessage botMsg = new ChatMessage("model", response);
        history.add(botMsg);

        callback.onResponse(botMsg);
    }

    @Override
    public List<ChatMessage> getHistory(String recipeId) {
        return getOrCreateHistory(recipeId);
    }

    @Override
    public void clearHistory(String recipeId) {
        conversationHistory.remove(recipeId);
    }

    private List<ChatMessage> getOrCreateHistory(String recipeId) {
        if (!conversationHistory.containsKey(recipeId)) {
            conversationHistory.put(recipeId, new ArrayList<>());
        }
        return conversationHistory.get(recipeId);
    }

    private String generateMockResponse(Recipe recipe, String userMessage) {
        String lower = userMessage.toLowerCase();

        if (lower.contains("substitute") || lower.contains("replace")) {
            return "[Mock] Great question about substitutions! This recipe \""
                    + recipe.getTitle() + "\" has "
                    + recipe.getIngredients().size()
                    + " ingredients. In a real session, I'd suggest specific alternatives.";
        }

        if (lower.contains("equipment") || lower.contains("tool")) {
            return "[Mock] For \"" + recipe.getTitle()
                    + "\", you'd typically need basic kitchen equipment. "
                    + "Connect a Gemini API key for detailed recommendations.";
        }

        if (lower.contains("calorie") || lower.contains("nutrition")) {
            return "[Mock] This recipe has an estimated "
                    + (int) recipe.getEstimatedCalories()
                    + " kcal. Connect a Gemini API key for detailed nutritional breakdown.";
        }

        return "[Mock] I'm a mock cooking assistant for \""
                + recipe.getTitle() + "\". "
                + "Configure a Gemini API key in local.properties for real AI responses.";
    }
}
