package com.example.foodtok.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodtok.R;
import com.example.foodtok.adapters.FeedAdapter;
import com.example.foodtok.models.Recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        setupRecyclerView(view);
        return view;
    }

    private void setupRecyclerView(View view) {
        List<Recipe> recipes = initializeMockData();
        RecyclerView recyclerView = view.findViewById(R.id.feedRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new FeedAdapter(recipes));
    }

    private List<Recipe> initializeMockData() {
        List<Recipe> recipes = new ArrayList<>();
        recipes.add(new Recipe(
                "1",
                "Spicy Ramen Bowl",
                "",
                Arrays.asList("#ramen", "#spicy", "#japanese"),
                Arrays.asList("noodles", "broth", "chili oil", "egg")
        ));
        recipes.add(new Recipe(
                "2",
                "Avocado Toast",
                "",
                Arrays.asList("#breakfast", "#healthy", "#avocado"),
                Arrays.asList("sourdough", "avocado", "lemon", "salt")
        ));
        recipes.add(new Recipe(
                "3",
                "Chocolate Lava Cake",
                "",
                Arrays.asList("#dessert", "#chocolate", "#baking"),
                Arrays.asList("dark chocolate", "butter", "eggs", "flour", "sugar")
        ));
        return recipes;
    }
}
