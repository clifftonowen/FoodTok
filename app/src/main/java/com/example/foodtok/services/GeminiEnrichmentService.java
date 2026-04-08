package com.example.foodtok.services;

import androidx.annotation.NonNull;

import com.example.foodtok.models.Ingredient;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.RecipeEnrichment;
import com.example.foodtok.models.dto.GeminiRequest;
import com.example.foodtok.models.dto.GeminiResponse;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Real implementation of IRecipeEnrichmentService using Gemini REST API.
 *
 * Sends a one-shot prompt asking Gemini to analyze a recipe and return
 * structured JSON with detected allergens, generated instructions,
 * estimated calories, and suggested tags.
 *
 * Results are cached per recipe to avoid redundant API calls.
 *
 * OOP: Implements IRecipeEnrichmentService (polymorphism).
 * Encapsulation: API key, cache, and prompt logic are private.
 */
public class GeminiEnrichmentService implements IRecipeEnrichmentService {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";

    private final String apiKey;
    private final GeminiApi geminiApi;
    private final Map<String, RecipeEnrichment> cache;

    public GeminiEnrichmentService(String apiKey) {
        this.apiKey = apiKey;
        this.cache = new HashMap<>();

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.NONE);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.geminiApi = retrofit.create(GeminiApi.class);
    }

    @Override
    public void enrichRecipe(Recipe recipe, EnrichmentCallback callback) {
        String recipeId = recipe.getId();

        // Return cached result if available
        if (cache.containsKey(recipeId)) {
            callback.onEnriched(cache.get(recipeId));
            return;
        }

        // Build the enrichment prompt
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(recipe);
        GeminiRequest request = GeminiRequest.singleTurn(systemPrompt, userPrompt);

        // Make async API call
        geminiApi.generateContent(apiKey, request).enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeminiResponse> call,
                                   @NonNull Response<GeminiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String text = response.body().getText();
                    if (text != null) {
                        RecipeEnrichment enrichment = parseEnrichment(recipeId, text);
                        if (enrichment != null) {
                            cache.put(recipeId, enrichment);
                            callback.onEnriched(enrichment);
                        } else {
                            callback.onError("Failed to parse enrichment response");
                        }
                    } else {
                        callback.onError("Empty response from Gemini");
                    }
                } else {
                    callback.onError("Gemini API error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<GeminiResponse> call,
                                  @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    @Override
    public RecipeEnrichment getCachedEnrichment(String recipeId) {
        return cache.get(recipeId);
    }

    @Override
    public boolean isEnriched(String recipeId) {
        return cache.containsKey(recipeId);
    }

    // --- Private helpers ---

    private String buildSystemPrompt() {
        return "You are a culinary analysis AI. Analyze recipes and return ONLY valid JSON "
                + "(no markdown, no code fences, no explanation). "
                + "The JSON must have this exact structure:\n"
                + "{\n"
                + "  \"detected_allergens\": [\"ingredient names that are common allergens\"],\n"
                + "  \"instructions\": [\"Step 1...\", \"Step 2...\"],\n"
                + "  \"estimated_calories\": 450,\n"
                + "  \"suggested_tags\": [\"tag1\", \"tag2\"]\n"
                + "}\n"
                + "Common allergens include: milk, eggs, fish, shellfish, tree nuts, "
                + "peanuts, wheat, soybeans, sesame. Flag any ingredient that contains "
                + "or is derived from these.";
    }

    private String buildUserPrompt(Recipe recipe) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyze this recipe:\n\n");
        sb.append("Title: ").append(recipe.getTitle()).append("\n");

        List<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients != null && !ingredients.isEmpty()) {
            sb.append("Ingredients:\n");
            for (Ingredient ing : ingredients) {
                sb.append("- ").append(ing.getName()).append("\n");
            }
        }

        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            sb.append("\nDescription: ").append(recipe.getDescription()).append("\n");
        }

        sb.append("\nGenerate allergen warnings, cooking instructions, ")
          .append("calorie estimate, and relevant tags.");

        return sb.toString();
    }

    /**
     * Parses the JSON response from Gemini into a RecipeEnrichment object.
     * Handles potential variations in the response format.
     */
    private RecipeEnrichment parseEnrichment(String recipeId, String jsonText) {
        try {
            // Strip markdown code fences if Gemini adds them despite instructions
            String cleaned = jsonText.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").trim();
            }

            JsonObject json = JsonParser.parseString(cleaned).getAsJsonObject();

            List<String> allergens = parseStringArray(json, "detected_allergens");
            List<String> instructions = parseStringArray(json, "instructions");
            double calories = json.has("estimated_calories")
                    ? json.get("estimated_calories").getAsDouble() : 0;
            List<String> tags = parseStringArray(json, "suggested_tags");

            return new RecipeEnrichment(recipeId, allergens, instructions, calories, tags);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> parseStringArray(JsonObject json, String key) {
        List<String> result = new ArrayList<>();
        if (json.has(key) && json.get(key).isJsonArray()) {
            JsonArray array = json.getAsJsonArray(key);
            for (JsonElement element : array) {
                result.add(element.getAsString());
            }
        }
        return result;
    }
}
