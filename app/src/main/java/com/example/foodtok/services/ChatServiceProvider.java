package com.example.foodtok.services;

import com.example.foodtok.util.Constants;

/**
 * Factory for IChatService instances.
 * Auto-selects SupabaseChatService when the app is pointed at a Supabase
 * project (the Gemini key now lives server-side in an Edge Function),
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
      if (Constants.isAiProxyConfigured()) {
        chatService = new SupabaseChatService();
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
