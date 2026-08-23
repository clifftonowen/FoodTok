package com.example.foodtok.services;

import com.example.foodtok.models.dto.AiChatRequest;
import com.example.foodtok.models.dto.AiEnrichRequest;
import com.example.foodtok.models.dto.AiTextResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Retrofit interface for the Supabase Edge Functions that proxy Gemini.
 *
 * <p>Base URL: {@code Constants.FUNCTIONS_BASE_URL}. The Gemini API key lives as a
 * Supabase secret on the server and never reaches the device.
 */
public interface AiProxyApi {

  @POST("gemini-chat")
  Call<AiTextResponse> chat(@Body AiChatRequest request);

  @POST("gemini-enrich")
  Call<AiTextResponse> enrich(@Body AiEnrichRequest request);
}
