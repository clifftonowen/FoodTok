package com.example.foodtok.models.dto;

import java.util.List;

/**
 * Response from the Gemini REST API (generateContent endpoint).
 * Parses the nested JSON: candidates[0].content.parts[0].text
 */
public class GeminiResponse {

    private List<Candidate> candidates;

    /**
     * Extracts the text from the first candidate's first part.
     * Returns null if the response is empty or malformed.
     */
    public String getText() {
        if (candidates == null || candidates.isEmpty()) return null;
        Candidate first = candidates.get(0);
        if (first.content == null || first.content.parts == null || first.content.parts.isEmpty()) {
            return null;
        }
        return first.content.parts.get(0).text;
    }

    public List<Candidate> getCandidates() { return candidates; }

    public static class Candidate {
        private Content content;
        private String finishReason;

        public Content getContent() { return content; }
        public String getFinishReason() { return finishReason; }
    }

    public static class Content {
        private String role;
        private List<Part> parts;

        public String getRole() { return role; }
        public List<Part> getParts() { return parts; }
    }

    public static class Part {
        private String text;

        public String getText() { return text; }
    }
}
