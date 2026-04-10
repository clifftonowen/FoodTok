package com.example.foodtok.services;

import android.content.Context;
import android.net.Uri;

import com.example.foodtok.models.Ingredient;
import com.example.foodtok.models.Recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** In-memory mock implementation of {@link IRecipeService} for offline testing. */
public class MockRecipeService implements IRecipeService {

  private final List<Recipe> mockRecipes;

  public MockRecipeService() {
    mockRecipes = buildMockData();
  }

  @Override
  public void getFeedRecipes(int page, int pageSize,
      RecipeListCallback callback) {
    int from = page * pageSize;
    if (from >= mockRecipes.size()) {
      callback.onSuccess(new ArrayList<>());
      return;
    }
    int to = Math.min(from + pageSize, mockRecipes.size());
    callback.onSuccess(new ArrayList<>(mockRecipes.subList(from, to)));
  }

  @Override
  public void getRecipeById(String recipeId, RecipeCallback callback) {
    for (Recipe r : mockRecipes) {
      if (r.getId().equals(recipeId)) {
        callback.onSuccess(r);
        return;
      }
    }
    callback.onError("Recipe not found");
  }

  @Override
  public void uploadRecipe(Context context, Uri videoUri, String title,
      String description, String[] tags, int prepTimeMinutes,
      int cookTimeMinutes, double estimatedCalories,
      RecipeCallback callback) {
    Recipe recipe = new Recipe(UUID.randomUUID().toString(),
        title, videoUri.toString(), Arrays.asList(tags),
        new ArrayList<>());
    recipe.setDescription(description);
    recipe.setPrepTimeMinutes(prepTimeMinutes);
    recipe.setCookTimeMinutes(cookTimeMinutes);
    recipe.setEstimatedCalories(estimatedCalories);
    recipe.setAuthorName("Mock User");
    mockRecipes.add(0, recipe);
    callback.onSuccess(recipe);
  }

  @Override
  public void searchByIngredients(Set<String> ingredientNames,
      RecipeListCallback callback) {
    List<Recipe> matched = new ArrayList<>();
    for (Recipe r : mockRecipes) {
      if (r.countMatchingIngredients(ingredientNames) > 0) {
        matched.add(r);
      }
    }
    Collections.sort(matched, (a, b) ->
        Integer.compare(
            b.countMatchingIngredients(ingredientNames),
            a.countMatchingIngredients(ingredientNames)));
    callback.onSuccess(matched);
  }

  private List<Recipe> buildMockData() {
    List<Recipe> recipes = new ArrayList<>();

    Recipe ramen = new Recipe("1", "Spicy Ramen Bowl",
        "https://example.com/ramen.mp4",
        Arrays.asList("#ramen", "#spicy", "#japanese"),
        Arrays.asList(
            new Ingredient("noodles", 138),
            new Ingredient("broth", 15),
            new Ingredient("chili oil", 40),
            new Ingredient("egg", 78)));
    ramen.setAuthorName("Chef Kenji");
    ramen.setPrepTimeMinutes(10);
    ramen.setCookTimeMinutes(20);
    ramen.setEstimatedCalories(450);

    Recipe toast = new Recipe("2", "Avocado Toast",
        "https://example.com/avocado.mp4",
        Arrays.asList("#breakfast", "#healthy", "#avocado"),
        Arrays.asList(
            new Ingredient("sourdough", 120),
            new Ingredient("avocado", 160),
            new Ingredient("lemon", 12),
            new Ingredient("salt", 0)));
    toast.setAuthorName("Brunch Queen");
    toast.setPrepTimeMinutes(5);
    toast.setCookTimeMinutes(3);
    toast.setEstimatedCalories(292);

    Recipe cake = new Recipe("3", "Chocolate Lava Cake",
        "https://example.com/lavacake.mp4",
        Arrays.asList("#dessert", "#chocolate", "#baking"),
        Arrays.asList(
            new Ingredient("dark chocolate", 170),
            new Ingredient("butter", 102),
            new Ingredient("eggs", 78),
            new Ingredient("flour", 110),
            new Ingredient("sugar", 50)));
    cake.setAuthorName("Pastry Pro");
    cake.setPrepTimeMinutes(15);
    cake.setCookTimeMinutes(12);
    cake.setEstimatedCalories(510);

    recipes.add(ramen);
    recipes.add(toast);
    recipes.add(cake);
    return recipes;
  }
}
