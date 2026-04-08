package com.example.foodtok.services;

import com.example.foodtok.BuildConfig;

/**
 * Factory for IChatService instances.
 * Auto-selects GeminiChatService when an API key is configured,
 * falls back to MockChatService otherwise.
 *
 * Follows the same Singleton Provider pattern as AuthServiceProvider
 * and InteractionServiceProvider.
 */
public final class ChatServiceProvider {

    private static IChatService chatService;

    private ChatServiceProvider() {
        // prevent instantiation
    }

    public static IChatService getChatService() {
        if (chatService == null) {
            String key = BuildConfig.GEMINI_API_KEY;
            if (key != null && !key.isEmpty()) {
                chatService = new GeminiChatService(key);
            } else {
                chatService = new MockChatService();
            }
        }
        return chatService;
    }

    public static void setChatService(IChatService service) {
        chatService = service;
    }
}
