package com.example.foodtok.models.dto;

/**
 * Response body returned by both Supabase AI proxy functions.
 *
 * <p>Carries the model's raw text verbatim. For enrichment that text is a JSON document
 * which the client parses itself, so the existing parsing and caching logic is unaffected
 * by the move to a server-side proxy.
 *
 * <p>{@link #getText()} intentionally mirrors the old {@code GeminiResponse.getText()}
 * signature so call sites migrate unchanged.
 */
public class AiTextResponse {

  private String text;

  /** @return the model's raw response text, or null if the field was absent */
  public String getText() {
    return text;
  }
}
