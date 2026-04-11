package com.example.foodtok.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
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
import com.example.foodtok.adapters.GridAdapter;
import com.example.foodtok.adapters.IngredientSuggestionAdapter;
import com.example.foodtok.data.Trie;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.dto.IngredientDto;
import com.example.foodtok.models.dto.TagDto;
import com.example.foodtok.services.RecipeListCallback;
import com.example.foodtok.services.RecipeServiceProvider;
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.util.ApiClient;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Search tab fragment with Trie-powered autocomplete over both
 * ingredient names and recipe tags. Users type a prefix, pick a
 * suggestion, and the selected tokens are OR-matched against recipes,
 * ranked by how many distinct tokens each recipe matches.
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

  private final Trie searchTrie = new Trie();
  private final Set<String> selectedTokens = new HashSet<>();

  private IngredientSuggestionAdapter suggestionAdapter;
  private GridAdapter resultAdapter;
  private final List<Recipe> resultRecipes = new ArrayList<>();

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
    loadTags();

    tvSearchAction.setOnClickListener(v -> performSearch());
  }

  private void setupSuggestions() {
    suggestionAdapter = new IngredientSuggestionAdapter();
    rvSuggestions.setLayoutManager(
        new LinearLayoutManager(requireContext()));
    rvSuggestions.setAdapter(suggestionAdapter);

    suggestionAdapter.setListener(name -> {
      addSelectedToken(name);
      etSearch.setText("");
      rvSuggestions.setVisibility(View.GONE);
    });
  }

  private void setupResults() {
    resultAdapter = new GridAdapter(resultRecipes, this::openFeedAt);
    rvSearchResults.setLayoutManager(
        new GridLayoutManager(requireContext(), 2));
    rvSearchResults.setAdapter(resultAdapter);
  }

  /** Opens the full-screen doomscroll feed starting at the tapped recipe. */
  private void openFeedAt(int position) {
    GridFeedFragment.setPendingRecipes(new ArrayList<>(resultRecipes));
    Bundle args = new Bundle();
    args.putInt("startPosition", position);
    GridFeedFragment feedFragment = new GridFeedFragment();
    feedFragment.setArguments(args);

    requireActivity().getSupportFragmentManager()
        .beginTransaction()
        .setCustomAnimations(
            R.anim.feed_enter,
            R.anim.feed_exit,
            R.anim.feed_enter,
            R.anim.feed_exit)
        .replace(R.id.fragmentContainer, feedFragment)
        .addToBackStack(null)
        .commit();
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
            searchTrie.autocomplete(prefix, MAX_SUGGESTIONS);
        // Remove already-selected tokens from suggestions
        results.removeAll(selectedTokens);
        if (results.isEmpty()) {
          rvSuggestions.setVisibility(View.GONE);
        } else {
          suggestionAdapter.setSuggestions(results);
          rvSuggestions.setVisibility(View.VISIBLE);
        }
      }
    });

    // Trigger the search when the user hits Done / Search / Enter on the
    // soft keyboard, so they don't have to dismiss it and tap the button.
    etSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
    etSearch.setOnEditorActionListener((v, actionId, event) -> {
      boolean isActionKey = actionId == EditorInfo.IME_ACTION_SEARCH
          || actionId == EditorInfo.IME_ACTION_DONE
          || actionId == EditorInfo.IME_ACTION_GO;
      boolean isEnterKey = event != null
          && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
          && event.getAction() == KeyEvent.ACTION_DOWN;
      if (isActionKey || isEnterKey) {
        rvSuggestions.setVisibility(View.GONE);
        hideKeyboard();
        performSearch();
        return true;
      }
      return false;
    });
  }

  private void hideKeyboard() {
    InputMethodManager imm = (InputMethodManager)
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imm != null && etSearch != null) {
      imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
    }
    etSearch.clearFocus();
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
                  searchTrie.insert(dto.name.toLowerCase());
                }
              }
              Log.d(TAG, "Loaded ingredients into Trie; size="
                  + searchTrie.size());
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

  /**
   * Fetches every distinct recipe tag from the {@code distinct_tags}
   * SQL view and inserts them into the same Trie used for ingredient
   * autocomplete. Tags and ingredient names share one suggestion
   * namespace.
   */
  private void loadTags() {
    SupabaseApi api =
        ApiClient.getRestClient().create(SupabaseApi.class);
    api.getAllTags("name", "name.asc")
        .enqueue(new Callback<List<TagDto>>() {
          @Override
          public void onResponse(Call<List<TagDto>> call,
              Response<List<TagDto>> response) {
            if (response.isSuccessful()
                && response.body() != null) {
              for (TagDto dto : response.body()) {
                if (dto.name == null) {
                  continue;
                }
                String normalized = dto.name.trim().toLowerCase();
                if (normalized.isEmpty()) {
                  continue;
                }
                searchTrie.insert(normalized);
              }
              Log.d(TAG, "Loaded tags from distinct_tags view; "
                  + "Trie size=" + searchTrie.size());
            } else {
              Log.w(TAG, "Failed to load tags: "
                  + response.code());
            }
          }

          @Override
          public void onFailure(Call<List<TagDto>> call,
              Throwable t) {
            Log.w(TAG, "Tag load error", t);
          }
        });
  }

  /** Adds a closable chip for the selected token (tag or ingredient). */
  private void addSelectedToken(String name) {
    String lower = name.toLowerCase();
    if (selectedTokens.contains(lower)) {
      return;
    }
    selectedTokens.add(lower);

    Chip chip = new Chip(requireContext());
    chip.setText(name);
    chip.setCloseIconVisible(true);
    chip.setOnCloseIconClickListener(v -> {
      selectedTokens.remove(lower);
      chipGroupSelected.removeView(chip);
    });
    chipGroupSelected.addView(chip);
  }

  /** Searches recipes using the selected tokens (tags + ingredients). */
  private void performSearch() {
    if (selectedTokens.isEmpty()) {
      Toast.makeText(requireContext(),
          "Select at least one tag or ingredient",
          Toast.LENGTH_SHORT).show();
      return;
    }

    RecipeServiceProvider.getRecipeService()
        .searchByIngredients(selectedTokens,
            new RecipeListCallback() {
              @Override
              public void onSuccess(List<Recipe> recipes) {
                if (!isAdded()) {
                  return;
                }
                requireActivity().runOnUiThread(() -> {
                  if (recipes.isEmpty()) {
                    tvEmptyState.setText(
                        "No recipes matched those tags or "
                            + "ingredients.");
                    tvEmptyState.setVisibility(View.VISIBLE);
                    rvSearchResults.setVisibility(View.GONE);
                  } else {
                    tvEmptyState.setVisibility(View.GONE);
                    rvSearchResults.setVisibility(View.VISIBLE);
                    resultRecipes.clear();
                    resultRecipes.addAll(recipes);
                    resultAdapter.notifyDataSetChanged();
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