package com.example.foodtok.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

    private static final float ALPHA_ACTIVE = 1.0f;
    private static final float ALPHA_INACTIVE = 0.45f;

    private ViewPager2 feedViewPager;
    private FeedAdapter feedAdapter;

    private TextView navIngredients;
    private TextView navForYou;
    private TextView navChat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        setupTopNav(view);
        setupFeedPager(view);
        return view;
    }

    private void setupTopNav(View view) {
        navIngredients = view.findViewById(R.id.navIngredients);
        navForYou = view.findViewById(R.id.navForYou);
        navChat = view.findViewById(R.id.navChat);

        // Default active tab = For You
        updateNavStyling(1);

        navIngredients.setOnClickListener(v -> {
            if (feedAdapter != null) {
                feedAdapter.navigateCurrentPageTo(0);
            }
        });

        navForYou.setOnClickListener(v -> {
            if (feedAdapter != null) {
                feedAdapter.navigateCurrentPageTo(1);
            }
        });

        navChat.setOnClickListener(v -> {
            if (feedAdapter != null) {
                feedAdapter.navigateCurrentPageTo(2);
            }
        });
    }

    private void updateNavStyling(int activePage) {
        TextView[] tabs = {navIngredients, navForYou, navChat};
        for (int i = 0; i < tabs.length; i++) {
            boolean active = (i == activePage);
            tabs[i].setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
            tabs[i].animate()
                    .alpha(active ? ALPHA_ACTIVE : ALPHA_INACTIVE)
                    .setDuration(150)
                    .start();
        }
    }

    private void setupFeedPager(View view) {
        List<Recipe> recipes = initializeMockData();

        feedViewPager = view.findViewById(R.id.feedViewPager);

        feedAdapter = new FeedAdapter(recipes, new OnRecipeInteractionListener() {
            @Override
            public void onLikeClicked(Recipe recipe) {
                InteractionServiceProvider.getInteractionService()
                        .likeRecipe(recipe.getId(), new InteractionCallback() {
                            @SuppressLint("NotifyDataSetChanged")
                            @Override
                            public void onSuccess() {
                                feedAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onError(String message) {
                                if ("Please log in first".equals(message)) {
                                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                                    startActivity(intent);
                                } else {
                                    showToast(message);
                                }
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
                                feedAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onError(String message) {
                                if ("Please log in first".equals(message)) {
                                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                                    startActivity(intent);
                                } else {
                                    showToast(message);
                                }
                            }
                        });
            }
        });

        feedAdapter.setParentVerticalPager(feedViewPager);

        // Update nav style when horizontal page changes inside current recipe card
        feedAdapter.setOnHorizontalPageChangedListener((adapterPosition, horizontalPage) -> {
            if (adapterPosition == feedViewPager.getCurrentItem()) {
                updateNavStyling(horizontalPage);
            }
        });

        // Reset top nav to "For You" when user swipes to a different recipe
        feedViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateNavStyling(1);
            }
        });

        feedViewPager.setAdapter(feedAdapter);
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private List<Recipe> initializeMockData() {
        List<Recipe> recipes = new ArrayList<>();

        Recipe ramen = new Recipe(
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
        );
        ramen.setAuthorName("Chef Kenji");
        ramen.setPrepTimeMinutes(10);
        ramen.setCookTimeMinutes(20);
        ramen.setEstimatedCalories(450);

        Recipe toast = new Recipe(
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
        );
        toast.setAuthorName("Brunch Queen");
        toast.setPrepTimeMinutes(5);
        toast.setCookTimeMinutes(3);
        toast.setEstimatedCalories(292);


        Recipe cake = new Recipe(
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
        );
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