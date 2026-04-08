package com.example.foodtok.services;

import com.example.foodtok.models.dto.GeminiRequest;
import com.example.foodtok.models.dto.GeminiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit interface for the Gemini REST API.
 * Base URL: https://generativelanguage.googleapis.com/
 *
 * Used by both GeminiChatService (conversation) and
 * GeminiEnrichmentService (one-shot analysis).
 */
public interface GeminiApi {

    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    Call<GeminiResponse> generateContent(
            @Query("key") String apiKey,
            @Body GeminiRequest request
    );
}
