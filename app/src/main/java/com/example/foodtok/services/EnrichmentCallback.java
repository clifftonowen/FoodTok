package com.example.foodtok.services;

import com.example.foodtok.models.RecipeEnrichment;

/**
 * Callback for asynchronous recipe enrichment responses.
 * Follows the same pattern as InteractionCallback and ChatCallback.
 */
public interface EnrichmentCallback {
    void onEnriched(RecipeEnrichment enrichment);
    void onError(String message);
}
