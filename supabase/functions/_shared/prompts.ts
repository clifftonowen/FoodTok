/**
 * Prompt construction, ported from the Android client.
 *
 * These prompts used to be built on the device and sent to Gemini directly.
 * They now live server-side so the request body the client sends is a typed,
 * constrained recipe object rather than a free-form model prompt. That is what
 * stops the function from being usable as a general-purpose LLM proxy by anyone
 * who extracts the anon key from the APK.
 *
 * Ported verbatim from GeminiChatService.buildSystemPrompt and
 * GeminiEnrichmentService.buildSystemPrompt / buildUserPrompt so model output
 * does not change as part of this migration.
 */

import type { CleanRecipe } from "./sanitize.ts";

/**
 * Chat system prompt.
 *
 * NOTE: the "[ALLERGEN]" marker is appended to every ingredient unconditionally.
 * That is a pre-existing bug in GeminiChatService.buildSystemPrompt (line 151),
 * carried over deliberately so this migration is behaviour-neutral. Fix it in a
 * separate change, not here.
 */
export function buildChatSystemPrompt(recipe: CleanRecipe): string {
  let sb = `You are a helpful cooking assistant for the recipe "${recipe.title}"`;

  if (recipe.authorName.length > 0) {
    sb += ` by ${recipe.authorName}`;
  }
  sb += ".\n\n";

  if (recipe.ingredients.length > 0) {
    sb += "Ingredients:\n";
    for (const ing of recipe.ingredients) {
      sb += `- ${ing.name}`;
      sb += ` (${Math.trunc(ing.calories)} kcal)`;
      sb += " [ALLERGEN]";
      sb += "\n";
    }
    sb += "\n";
  }

  if (recipe.prepTimeMinutes > 0 || recipe.cookTimeMinutes > 0) {
    sb += `Prep: ${recipe.prepTimeMinutes} min | `;
    sb += `Cook: ${recipe.cookTimeMinutes} min`;
    if (recipe.estimatedCalories > 0) {
      sb += ` | ~${Math.trunc(recipe.estimatedCalories)} kcal total`;
    }
    sb += "\n\n";
  }

  if (recipe.tags.length > 0) {
    sb += `Tags: ${recipe.tags.join(", ")}\n\n`;
  }

  sb += "Help the user with ingredient substitutions, cooking techniques, " +
    "equipment needed, nutritional information, dietary modifications, " +
    "and step-by-step guidance. Keep answers concise and practical. " +
    "If asked about something unrelated to cooking or this recipe, " +
    "politely redirect the conversation.";

  return sb;
}

/** Enrichment system prompt. Defines the JSON contract the client parses. */
export function buildEnrichSystemPrompt(): string {
  return "You are a culinary analysis AI. Analyze recipes and return ONLY valid JSON " +
    "(no markdown, no code fences, no explanation). " +
    "The JSON must have this exact structure:\n" +
    "{\n" +
    '  "detected_allergens": ["ingredient names that are common allergens"],\n' +
    '  "instructions": ["Step 1...", "Step 2..."],\n' +
    '  "estimated_calories": 450,\n' +
    '  "suggested_tags": ["tag1", "tag2"]\n' +
    "}\n" +
    "Common allergens include: milk, eggs, fish, shellfish, tree nuts, " +
    "peanuts, wheat, soybeans, sesame. Flag any ingredient that contains " +
    "or is derived from these.";
}

/** Enrichment user prompt describing the recipe to analyse. */
export function buildEnrichUserPrompt(recipe: CleanRecipe): string {
  let sb = "Analyze this recipe:\n\n";
  sb += `Title: ${recipe.title}\n`;

  if (recipe.ingredients.length > 0) {
    sb += "Ingredients:\n";
    for (const ing of recipe.ingredients) {
      sb += `- ${ing.name}\n`;
    }
  }

  if (recipe.description.length > 0) {
    sb += `\nDescription: ${recipe.description}\n`;
  }

  sb += "\nGenerate allergen warnings, cooking instructions, " +
    "calorie estimate, and relevant tags.";

  return sb;
}
