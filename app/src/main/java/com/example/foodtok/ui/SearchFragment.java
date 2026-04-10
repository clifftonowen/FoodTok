package com.example.foodtok.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodtok.R;
import com.example.foodtok.adapters.IngredientSuggestionAdapter;
import com.example.foodtok.adapters.SearchResultAdapter;
import com.example.foodtok.data.Trie;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.dto.IngredientDto;
import com.example.foodtok.services.RecipeListCallback;
import com.example.foodtok.services.RecipeServiceProvider;
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.util.ApiClient;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Search tab fragment with Trie-powered ingredient autocomplete.
 * Users type ingredient names, select from suggestions, then
 * search for recipes ranked by ingredient match count.
 */
public class SearchFragment extends Fragment {

  private static final String TAG = "SearchFragment";
  private static final int MAX_SUGGESTIONS = 10;

  private EditText etSearch;
  private TextView tvSearchAction;
  private TextView tvEmptyState;
  private RecyclerView rvSuggestions;
  private RecyclerView rvSearchResults;
  private ChipGroup chipGroupSelected;

  private final Trie ingredientTrie = new Trie();
  private final Set<String> selectedIngredients = new HashSet<>();

  private IngredientSuggestionAdapter suggestionAdapter;
  private SearchResultAdapter resultAdapter;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(
        R.layout.fragment_search, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view,
      @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    etSearch = view.findViewById(R.id.etSearch);
    tvSearchAction = view.findViewById(R.id.tvSearchAction);
    tvEmptyState = view.findViewById(R.id.tvEmptyState);
    rvSuggestions = view.findViewById(R.id.rvSuggestions);
    rvSearchResults = view.findViewById(R.id.rvSearchResults);
    chipGroupSelected = view.findViewById(R.id.chipGroupSelected);

    setupSuggestions();
    setupResults();
    setupSearchInput();
    loadIngredients();

    tvSearchAction.setOnClickListener(v -> performSearch());
  }

  private void setupSuggestions() {
    suggestionAdapter = new IngredientSuggestionAdapter();
    rvSuggestions.setLayoutManager(
        new LinearLayoutManager(requireContext()));
    rvSuggestions.setAdapter(suggestionAdapter);

    suggestionAdapter.setListener(name -> {
      addSelectedIngredient(name);
      etSearch.setText("");
      rvSuggestions.setVisibility(View.GONE);
    });
  }

  private void setupResults() {
    resultAdapter = new SearchResultAdapter();
    rvSearchResults.setLayoutManager(
        new GridLayoutManager(requireContext(), 2));
    rvSearchResults.setAdapter(resultAdapter);
  }

  private void setupSearchInput() {
    etSearch.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start,
          int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start,
          int before, int count) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        String prefix = s.toString().trim();
        if (prefix.isEmpty()) {
          rvSuggestions.setVisibility(View.GONE);
          return;
        }
        List<String> results =
            ingredientTrie.autocomplete(prefix, MAX_SUGGESTIONS);
        // Remove already-selected ingredients from suggestions
        results.removeAll(selectedIngredients);
        if (results.isEmpty()) {
          rvSuggestions.setVisibility(View.GONE);
        } else {
          suggestionAdapter.setSuggestions(results);
          rvSuggestions.setVisibility(View.VISIBLE);
        }
      }
    });
  }

  /** Fetches all ingredients from Supabase and inserts into the Trie. */
  private void loadIngredients() {
    SupabaseApi api =
        ApiClient.getRestClient().create(SupabaseApi.class);
    api.getAllIngredients("name", "name.asc")
        .enqueue(new Callback<List<IngredientDto>>() {
          @Override
          public void onResponse(Call<List<IngredientDto>> call,
              Response<List<IngredientDto>> response) {
            if (response.isSuccessful()
                && response.body() != null) {
              for (IngredientDto dto : response.body()) {
                if (dto.name != null) {
                  ingredientTrie.insert(dto.name);
                }
              }
              Log.d(TAG, "Loaded " + ingredientTrie.size()
                  + " ingredients into Trie");
            } else {
              Log.w(TAG, "Failed to load ingredients: "
                  + response.code());
            }
          }

          @Override
          public void onFailure(Call<List<IngredientDto>> call,
              Throwable t) {
            Log.w(TAG, "Ingredient load error", t);
          }
        });
  }

  /** Adds a closable chip for the selected ingredient. */
  private void addSelectedIngredient(String name) {
    String lower = name.toLowerCase();
    if (selectedIngredients.contains(lower)) {
      return;
    }
    selectedIngredients.add(lower);

    Chip chip = new Chip(requireContext());
    chip.setText(name);
    chip.setCloseIconVisible(true);
    chip.setOnCloseIconClickListener(v -> {
      selectedIngredients.remove(lower);
      chipGroupSelected.removeView(chip);
    });
    chipGroupSelected.addView(chip);
  }

  /** Searches recipes using the selected ingredients. */
  private void performSearch() {
    if (selectedIngredients.isEmpty()) {
      Toast.makeText(requireContext(),
          "Select at least one ingredient", Toast.LENGTH_SHORT)
          .show();
      return;
    }

    RecipeServiceProvider.getRecipeService()
        .searchByIngredients(selectedIngredients,
            new RecipeListCallback() {
              @Override
              public void onSuccess(List<Recipe> recipes) {
                if (!isAdded()) {
                  return;
                }
                requireActivity().runOnUiThread(() -> {
                  if (recipes.isEmpty()) {
                    tvEmptyState.setText(
                        "No recipes found with those ingredients.");
                    tvEmptyState.setVisibility(View.VISIBLE);
                    rvSearchResults.setVisibility(View.GONE);
                  } else {
                    tvEmptyState.setVisibility(View.GONE);
                    rvSearchResults.setVisibility(View.VISIBLE);
                    resultAdapter.setResults(
                        recipes, selectedIngredients);
                  }
                });
              }

              @Override
              public void onError(String message) {
                if (!isAdded()) {
                  return;
                }
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(),
                        message, Toast.LENGTH_SHORT).show());
              }
            });
  }
}