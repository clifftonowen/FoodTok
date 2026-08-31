package com.example.foodtok.services;

import com.example.foodtok.util.Constants;
import com.example.foodtok.util.PreviewMode;

/**
 * Factory for IRecipeEnrichmentService instances.
 *
 * <p>Preview mode always gets the mock, so a demo build never touches the network.
 * Otherwise auto-selects SupabaseEnrichmentService when the app is pointed at a Supabase
 * project (the Gemini key lives server-side in an Edge Function), and falls back to
 * MockEnrichmentService when there is no backend configured.
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
      if (PreviewMode.isEnabled()) {
        enrichmentService = new MockEnrichmentService();
        return enrichmentService;
      }
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
