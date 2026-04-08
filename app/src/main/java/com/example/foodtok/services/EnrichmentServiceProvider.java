package com.example.foodtok.services;

import com.example.foodtok.BuildConfig;

/**
 * Factory for IRecipeEnrichmentService instances.
 * Auto-selects GeminiEnrichmentService when an API key is configured,
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
            String key = BuildConfig.GEMINI_API_KEY;
            if (key != null && !key.isEmpty()) {
                enrichmentService = new GeminiEnrichmentService(key);
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
