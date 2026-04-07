package com.example.foodtok.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.foodtok.R;
import com.example.foodtok.adapters.FeedAdapter;
import com.example.foodtok.adapters.OnRecipeInteractionListener;
import com.example.foodtok.models.Ingredient;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.services.InteractionCallback;
import com.example.foodtok.services.InteractionServiceProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    private ViewPager2 feedViewPager;
    private FeedAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        setupFeedPager(view);
        return view;
    }

    private void setupFeedPager(View view) {
        List<Recipe> recipes = initializeMockData();

        feedViewPager = view.findViewById(R.id.feedViewPager);

        adapter = new FeedAdapter(recipes, new OnRecipeInteractionListener() {
            @Override
            public void onLikeClicked(Recipe recipe) {
                InteractionServiceProvider.getInteractionService()
                        .likeRecipe(recipe.getId(), new InteractionCallback() {
                            @SuppressLint("NotifyDataSetChanged")
                            @Override
                            public void onSuccess() {
                                adapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onError(String message) {
                                // If the service says "Please log in first", go to LoginActivity
                                if (message.equals("Please log in first")) {
                                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                                    startActivity(intent);
                                }
                                showToast(message);
                            }
                        });
            }

            @Override
            public void onCommentClicked(Recipe recipe) {
                showToast("Comment clicked for " + recipe.getTitle());
            }

            @Override
            public void onSaveClicked(Recipe recipe) {
                InteractionServiceProvider.getInteractionService()
                        .saveRecipe(recipe.getId(), new InteractionCallback() {
                            @SuppressLint("NotifyDataSetChanged")
                            @Override
                            public void onSuccess() {
                                adapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onError(String message) {
                                if(message.equals("Please log in first")){
                                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                                    startActivity(intent);
                                }else{
                                    showToast(message);
                                }
                            }
                        });
            }
        });

        adapter.setParentVerticalPager(feedViewPager);
        feedViewPager.setAdapter(adapter);
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private List<Recipe> initializeMockData() {
        List<Recipe> recipes = new ArrayList<>();
        recipes.add(new Recipe(
                "1",
                "Spicy Ramen Bowl",
                "https://example.com/ramen.mp4",
                Arrays.asList("#ramen", "#spicy", "#japanese"),
                Arrays.asList(
                        new Ingredient("noodles", 138, false),
                        new Ingredient("broth", 15, false),
                        new Ingredient("chili oil", 40, false),
                        new Ingredient("egg", 78, true)
                )
        ));
        recipes.add(new Recipe(
                "2",
                "Avocado Toast",
                "https://example.com/avocado.mp4",
                Arrays.asList("#breakfast", "#healthy", "#avocado"),
                Arrays.asList(
                        new Ingredient("sourdough", 120, true),
                        new Ingredient("avocado", 160, false),
                        new Ingredient("lemon", 12, false),
                        new Ingredient("salt", 0, false)
                )
        ));
        recipes.add(new Recipe(
                "3",
                "Chocolate Lava Cake",
                "https://example.com/lavacake.mp4",
                Arrays.asList("#dessert", "#chocolate", "#baking"),
                Arrays.asList(
                        new Ingredient("dark chocolate", 170, false),
                        new Ingredient("butter", 102, true),
                        new Ingredient("eggs", 78, true),
                        new Ingredient("flour", 110, true),
                        new Ingredient("sugar", 50, false)
                )
        ));
        return recipes;
    }
}