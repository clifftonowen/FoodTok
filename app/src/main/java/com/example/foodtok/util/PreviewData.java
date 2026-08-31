package com.example.foodtok.util;

import com.example.foodtok.models.dto.RecipeDto;
import com.example.foodtok.models.dto.UserDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Deterministic display records shared by connection-free screens. */
public final class PreviewData {

  public static final List<String> SEARCH_TERMS = Arrays.asList(
      "avocado", "chicken", "chocolate", "egg", "lemon", "noodles",
      "pasta", "ramen", "spicy", "quick", "vegetarian", "weeknight");

  private PreviewData() {}

  public static List<UserDto> users() {
    List<UserDto> users = new ArrayList<>();
    users.add(user("creator-kenji", "chefkenji", "Big bowls, bold broths."));
    users.add(user("creator-mina", "minamakes", "Korean comfort food at home."));
    users.add(user("creator-pastry", "pastrypro", "Dessert without the drama."));
    users.add(user("creator-table", "weeknighttable", "Dinner in thirty minutes."));
    return users;
  }

  public static List<RecipeDto> profileRecipes() {
    List<RecipeDto> recipes = new ArrayList<>();
    recipes.add(recipe("preview-1", "Miso Butter Mushrooms",
        new String[]{"quick", "umami", "vegetarian"}));
    recipes.add(recipe("preview-2", "Crispy Chilli Eggs",
        new String[]{"breakfast", "spicy", "easy"}));
    recipes.add(recipe("preview-3", "Sesame Cucumber Salad",
        new String[]{"fresh", "asian", "quick"}));
    recipes.add(recipe("preview-4", "Brown Butter Cookies",
        new String[]{"dessert", "baking"}));
    recipes.add(recipe("preview-5", "Garlic Scallion Noodles",
        new String[]{"noodles", "weeknight"}));
    recipes.add(recipe("preview-6", "Coconut Curry Bowl",
        new String[]{"curry", "comfort"}));
    return recipes;
  }

  private static UserDto user(String id, String username, String bio) {
    UserDto user = new UserDto();
    user.id = id;
    user.username = username;
    user.bio = bio;
    return user;
  }

  private static RecipeDto recipe(String id, String title, String[] tags) {
    RecipeDto recipe = new RecipeDto();
    recipe.id = id;
    recipe.authorId = "preview-user";
    recipe.title = title;
    recipe.description = "A sample recipe for the offline UI preview.";
    recipe.tags = tags;
    recipe.prepTimeMinutes = 10;
    recipe.cookTimeMinutes = 15;
    recipe.estimatedCalories = 360;
    recipe.author = user("preview-user", "yourkitchen", "Cooking one swipe at a time.");
    return recipe;
  }
}
