package com.example.foodtok.models.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Request body for the Gemini REST API (generateContent endpoint).
 * Maps to the JSON structure expected by:
 * POST generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent
 */
public class GeminiRequest {

    private Content systemInstruction;
    private List<Content> contents;

    private GeminiRequest() {}

    /**
     * Creates a request with a system prompt and conversation history.
     * Used by GeminiChatService for contextual recipe Q&A.
     */
    public static GeminiRequest create(String systemPrompt, List<Content> conversationHistory) {
        GeminiRequest request = new GeminiRequest();
        request.systemInstruction = Content.ofSystem(systemPrompt);
        request.contents = new ArrayList<>(conversationHistory);
        return request;
    }

    /**
     * Creates a simple single-turn request (no conversation history).
     * Used by GeminiEnrichmentService for one-shot analysis.
     */
    public static GeminiRequest singleTurn(String systemPrompt, String userMessage) {
        GeminiRequest request = new GeminiRequest();
        request.systemInstruction = Content.ofSystem(systemPrompt);
        request.contents = Collections.singletonList(Content.ofUser(userMessage));
        return request;
    }

    public Content getSystemInstruction() { return systemInstruction; }
    public List<Content> getContents() { return contents; }

    /**
     * Represents a single turn in the conversation.
     * Each Content has a role ("user" or "model") and a list of Parts.
     */
    public static class Content {
        private String role;
        private List<Part> parts;

        private Content() {}

        public static Content ofUser(String text) {
            Content c = new Content();
            c.role = "user";
            c.parts = Collections.singletonList(new Part(text));
            return c;
        }

        public static Content ofModel(String text) {
            Content c = new Content();
            c.role = "model";
            c.parts = Collections.singletonList(new Part(text));
            return c;
        }

        static Content ofSystem(String text) {
            Content c = new Content();
            c.parts = Collections.singletonList(new Part(text));
            return c;
        }

        public String getRole() { return role; }
        public List<Part> getParts() { return parts; }
    }

    /**
     * A single piece of content within a turn. Currently text-only;
     * can be extended for image/video when Gemini multimodal is wired.
     */
    public static class Part {
        private final String text;

        public Part(String text) {
            this.text = text;
        }

        public String getText() { return text; }
    }
}
