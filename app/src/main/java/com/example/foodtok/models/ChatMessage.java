package com.example.foodtok.models;

/**
 * Represents a single message in a recipe chat conversation.
 *
 * OOP: Encapsulation — all fields are final (immutable after creation).
 * Tell, Don't Ask — isUser() and getFormattedTime() encapsulate logic.
 * Follows the same immutable pattern as Ingredient.
 */
public class ChatMessage {

    private final String role;           // "user" or "model"
    private final String text;
    private final long timestampMillis;

    public ChatMessage(String role, String text) {
        if (role == null || (!role.equals("user") && !role.equals("model"))) {
            throw new IllegalArgumentException("Role must be 'user' or 'model'");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Message text cannot be empty");
        }
        this.role = role;
        this.text = text.trim();
        this.timestampMillis = System.currentTimeMillis();
    }

    // --- Query methods (Tell, Don't Ask) ---

    public boolean isUser() {
        return "user".equals(role);
    }

    public boolean isModel() {
        return "model".equals(role);
    }

    /**
     * Returns a human-readable relative timestamp.
     * Same pattern as Comment.getFormattedTime().
     */
    public String getFormattedTime() {
        long diffMs = System.currentTimeMillis() - timestampMillis;
        long diffSec = diffMs / 1000;
        long diffMin = diffSec / 60;
        long diffHr = diffMin / 60;

        if (diffSec < 60) return "just now";
        if (diffMin < 60) return diffMin + "m ago";
        if (diffHr < 24) return diffHr + "h ago";
        return (diffHr / 24) + "d ago";
    }

    // --- Getters (no setters — messages are immutable) ---

    public String getRole() { return role; }

    public String getText() { return text; }

    public long getTimestampMillis() { return timestampMillis; }
}
