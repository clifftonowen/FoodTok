package com.example.foodtok.services;

import com.example.foodtok.util.Constants;
import com.example.foodtok.util.PreviewMode;

/**
 * Factory for IChatService instances.
 *
 * <p>Preview mode always gets the mock, so a demo build never touches the network.
 * Otherwise auto-selects SupabaseChatService when the app is pointed at a Supabase
 * project (the Gemini key lives server-side in an Edge Function), and falls back to
 * MockChatService when there is no backend configured.
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
      if (PreviewMode.isEnabled()) {
        chatService = new MockChatService();
        return chatService;
      }
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
