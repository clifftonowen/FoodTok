package com.example.foodtok.models.dto;

import com.example.foodtok.models.Ingredient;
import com.example.foodtok.models.Recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe context sent to the Supabase AI proxy functions.
 *
 * <p>This is a deliberately narrow projection of {@link Recipe}: only the fields the
 * server-side prompt builders need are serialised. Keeping the wire format typed and
 * constrained is what prevents the Edge Functions being used as a general-purpose LLM
 * proxy by anyone who extracts the anon key from the APK.
 *
 * <p>Both the chat and enrichment functions accept this same shape; each ignores the
 * fields it does not use.
 */
public class AiRecipeContext {

  private final String title;
  private final String authorName;
  private final String description;
  private final List<AiIngredient> ingredients;
  private final int prepTimeMinutes;
  private final int cookTimeMinutes;
  private final double estimatedCalories;
  private final List<String> tags;

  private AiRecipeContext(String title, String authorName, String description,
      List<AiIngredient> ingredients, int prepTimeMinutes, int cookTimeMinutes,
      double estimatedCalories, List<String> tags) {
    this.title = title;
    this.authorName = authorName;
    this.description = description;
    this.ingredients = ingredients;
    this.prepTimeMinutes = prepTimeMinutes;
    this.cookTimeMinutes = cookTimeMinutes;
    this.estimatedCalories = estimatedCalories;
    this.tags = tags;
  }

  /**
   * Projects a domain {@link Recipe} onto the wire format.
   *
   * @param recipe the recipe to describe; must not be null
   * @return a serialisable context object for the AI proxy
   */
  public static AiRecipeContext from(Recipe recipe) {
    List<AiIngredient> mapped = new ArrayList<>();
    List<Ingredient> source = recipe.getIngredients();
    if (source != null) {
      for (Ingredient ingredient : source) {
        mapped.add(new AiIngredient(ingredient.getName(), ingredient.getCalories()));
      }
    }

    List<String> tags = recipe.getTags() != null
        ? new ArrayList<>(recipe.getTags())
        : new ArrayList<>();

    return new AiRecipeContext(
        recipe.getTitle(),
        recipe.getAuthorName(),
        recipe.getDescription(),
        mapped,
        recipe.getPrepTimeMinutes(),
        recipe.getCookTimeMinutes(),
        recipe.getEstimatedCalories(),
        tags);
  }

  /** Minimal ingredient projection: the name and calorie figure the prompts use. */
  public static class AiIngredient {

    private final String name;
    private final double calories;

    AiIngredient(String name, double calories) {
      this.name = name;
      this.calories = calories;
    }

    public String getName() {
      return name;
    }

    public double getCalories() {
      return calories;
    }
  }
}
