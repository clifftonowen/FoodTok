package com.example.foodtok.services;

import com.example.foodtok.models.ChatMessage;

/**
 * Callback for asynchronous chat responses.
 * Follows the same pattern as InteractionCallback.
 */
public interface ChatCallback {
    void onResponse(ChatMessage response);
    void onError(String message);
}
