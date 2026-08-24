package com.example.foodtok.services;

import com.example.foodtok.util.Constants;

/**
 * Factory for IRecipeEnrichmentService instances.
 * Auto-selects SupabaseEnrichmentService when the app is pointed at a Supabase
 * project (the Gemini key now lives server-side in an Edge Function),
 * falls back to MockEnrichmentService otherwise.
 *
 * Follows the same Singleton Provider pattern as AuthServiceProvider,
 * InteractionServiceProvider, and ChatServiceProvider.
 */
public final class EnrichmentServiceProvider {

  private static IRecipeEnrichmentService enrichmentService;

  private EnrichmentServiceProvider() {
    // prevent instantiation
  }

  public static IRecipeEnrichmentService getEnrichmentService() {
    if (enrichmentService == null) {
      if (Constants.isAiProxyConfigured()) {
        enrichmentService = new SupabaseEnrichmentService();
      } else {
        enrichmentService = new MockEnrichmentService();
      }
    }
    return enrichmentService;
  }

  public static void setEnrichmentService(IRecipeEnrichmentService service) {
    enrichmentService = service;
  }
}
