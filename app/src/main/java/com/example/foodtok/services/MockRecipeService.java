package com.example.foodtok.services;

import android.content.Context;
import android.net.Uri;

import com.example.foodtok.models.Ingredient;
import com.example.foodtok.models.IngredientInput;
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
      String description, String[] tags, List<IngredientInput> ingredients,
      int prepTimeMinutes, int cookTimeMinutes, double estimatedCalories,
      RecipeCallback callback) {
    List<Ingredient> domainIngredients = new ArrayList<>();
    if (ingredients != null) {
      for (IngredientInput input : ingredients) {
        domainIngredients.add(new Ingredient(
            input.getName(), 0, input.getQuantity(), false));
      }
    }
    Recipe recipe = new Recipe(UUID.randomUUID().toString(),
        title, videoUri.toString(), Arrays.asList(tags),
        domainIngredients);
    recipe.setDescription(description);
    recipe.setPrepTimeMinutes(prepTimeMinutes);
    recipe.setCookTimeMinutes(cookTimeMinutes);
    recipe.setEstimatedCalories(estimatedCalories);
    recipe.setAuthorName("Mock User");
    mockRecipes.add(0, recipe);
    callback.onSuccess(recipe);
  }

  @Override
  public void searchByIngredients(Set<String> searchTokens,
      RecipeListCallback callback) {
    List<Recipe> matched = new ArrayList<>();
    for (Recipe r : mockRecipes) {
      if (r.countMatchingTokens(searchTokens) > 0) {
        matched.add(r);
      }
    }
    Collections.sort(matched, (a, b) ->
        Integer.compare(
            b.countMatchingTokens(searchTokens),
            a.countMatchingTokens(searchTokens)));
    callback.onSuccess(matched);
  }

  private List<Recipe> buildMockData() {
    List<Recipe> recipes = new ArrayList<>();

    Recipe ramen = new Recipe("1", "Spicy Ramen Bowl",
        "",
        Arrays.asList("ramen", "spicy", "japanese"),
        Arrays.asList(
            new Ingredient("noodles", 138, "200g", false),
            new Ingredient("broth", 15, "500ml", false),
            new Ingredient("chili oil", 40, "1 tbsp", false),
            new Ingredient("egg", 78, "1 large", false)));
    ramen.setAuthorName("Chef Kenji");
    ramen.setAuthorId("creator-kenji");
    ramen.setDescription("Silky broth, jammy egg, and a hit of chilli oil in twenty minutes.");
    ramen.setPrepTimeMinutes(10);
    ramen.setCookTimeMinutes(20);
    ramen.setEstimatedCalories(450);

    Recipe toast = new Recipe("2", "Avocado Toast",
        "",
        Arrays.asList("breakfast", "quick", "avocado"),
        Arrays.asList(
            new Ingredient("sourdough", 120, "2 slices", false),
            new Ingredient("avocado", 160, "1 whole", false),
            new Ingredient("lemon", 12, "1/2", false),
            new Ingredient("salt", 0, "to taste", false)));
    toast.setAuthorName("Brunch Queen");
    toast.setAuthorId("creator-brunch");
    toast.setDescription("Crisp sourdough with lemony avocado and a pinch of chilli flakes.");
    toast.setPrepTimeMinutes(5);
    toast.setCookTimeMinutes(3);
    toast.setEstimatedCalories(292);

    Recipe cake = new Recipe("3", "Chocolate Lava Cake",
        "",
        Arrays.asList("dessert", "chocolate", "baking"),
        Arrays.asList(
            new Ingredient("dark chocolate", 170, "100g", false),
            new Ingredient("butter", 102, "80g", false),
            new Ingredient("eggs", 78, "2 large", false),
            new Ingredient("flour", 110, "40g", false),
            new Ingredient("sugar", 50, "50g", false)));
    cake.setAuthorName("Pastry Pro");
    cake.setAuthorId("creator-pastry");
    cake.setDescription("A restaurant-style molten centre with only seven pantry ingredients.");
    cake.setPrepTimeMinutes(15);
    cake.setCookTimeMinutes(12);
    cake.setEstimatedCalories(510);

    Recipe tacos = new Recipe("4", "Crispy Gochujang Tacos", "",
        Arrays.asList("korean", "fusion", "weeknight"),
        Arrays.asList(
            new Ingredient("chicken", 165, "300g", false),
            new Ingredient("gochujang", 45, "2 tbsp", false),
            new Ingredient("tortillas", 130, "6 small", false),
            new Ingredient("cabbage", 25, "1 cup", false)));
    tacos.setAuthorName("Mina Makes");
    tacos.setAuthorId("creator-mina");
    tacos.setDescription("Crunchy, spicy-sweet tacos finished with a bright sesame slaw.");
    tacos.setPrepTimeMinutes(15);
    tacos.setCookTimeMinutes(18);
    tacos.setEstimatedCalories(390);

    Recipe pasta = new Recipe("5", "One-Pan Lemon Pasta", "",
        Arrays.asList("pasta", "quick", "vegetarian"),
        Arrays.asList(
            new Ingredient("spaghetti", 210, "200g", false),
            new Ingredient("lemon", 12, "1 whole", false),
            new Ingredient("parmesan", 110, "50g grated", false),
            new Ingredient("spinach", 20, "2 cups", false)));
    pasta.setAuthorName("Weeknight Table");
    pasta.setAuthorId("creator-table");
    pasta.setDescription("Bright, creamy pasta with almost no washing up.");
    pasta.setPrepTimeMinutes(5);
    pasta.setCookTimeMinutes(15);
    pasta.setEstimatedCalories(430);

    recipes.add(ramen);
    recipes.add(toast);
    recipes.add(cake);
    recipes.add(tacos);
    recipes.add(pasta);
    return recipes;
  }
}
