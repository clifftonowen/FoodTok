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
import com.example.foodtok.adapters.UserSearchAdapter;
import com.example.foodtok.data.Trie;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.dto.IngredientDto;
import com.example.foodtok.models.dto.TagDto;
import com.example.foodtok.models.dto.UserDto;
import com.example.foodtok.services.RecipeListCallback;
import com.example.foodtok.services.RecipeServiceProvider;
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.util.ApiClient;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Search tab fragment with Trie-powered autocomplete over both
 * ingredient names and recipe tags, plus username search. Users
 * can toggle between "Recipes" and "Users" result modes.
 */
public class SearchFragment extends Fragment {

  private static final String TAG = "SearchFragment";
  private static final int MAX_SUGGESTIONS = 10;

  private EditText etSearch;
  private TextView tvSearchAction;
  private TextView tvEmptyState;
  private RecyclerView rvSuggestions;
  private RecyclerView rvSearchResults;
  private RecyclerView rvUserResults;
  private ChipGroup chipGroupSelected;
  private TextView tvTabRecipes;
  private TextView tvTabUsers;

  private final Trie searchTrie = new Trie();
  private final Trie usernameTrie = new Trie();
  private final Set<String> selectedTokens = new HashSet<>();

  /** true = Recipes mode, false = Users mode. */
  private boolean isRecipeMode = true;

  /**
   * One-shot tag handed in from elsewhere in the app (e.g. a clickable
   * hashtag in the feed). Consumed and cleared on next view creation.
   */
  private static String pendingTag;

  /** Stashes a tag for the next SearchFragment instance to auto-search. */
  public static void setPendingTag(String tag) {
    pendingTag = tag;
  }

  private IngredientSuggestionAdapter suggestionAdapter;
  private GridAdapter resultAdapter;
  private UserSearchAdapter userAdapter;
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
    rvUserResults = view.findViewById(R.id.rvUserResults);
    chipGroupSelected = view.findViewById(R.id.chipGroupSelected);
    tvTabRecipes = view.findViewById(R.id.tvTabRecipes);
    tvTabUsers = view.findViewById(R.id.tvTabUsers);

    setupTabs();
    setupSuggestions();
    setupResults();
    setupUserResults();
    setupSearchInput();
    loadIngredients();
    loadTags();
    loadUsernames();

    tvSearchAction.setOnClickListener(v -> performSearch());

    if (pendingTag != null) {
      String tag = pendingTag;
      pendingTag = null;
      addSelectedToken(tag);
      performSearch();
    }
  }

  private void setupTabs() {
    tvTabRecipes.setOnClickListener(v -> switchToRecipeMode());
    tvTabUsers.setOnClickListener(v -> switchToUserMode());
  }

  private void switchToRecipeMode() {
    if (isRecipeMode) {
      return;
    }
    isRecipeMode = true;
    tvTabRecipes.setBackgroundColor(
        getResources().getColor(R.color.foodtok_green));
    tvTabRecipes.setTextColor(
        getResources().getColor(R.color.foodtok_white));
    tvTabUsers.setBackgroundColor(
        getResources().getColor(R.color.foodtok_divider));
    tvTabUsers.setTextColor(
        getResources().getColor(R.color.foodtok_text_secondary));

    etSearch.setHint("Type an ingredient...");
    chipGroupSelected.setVisibility(View.VISIBLE);
    rvUserResults.setVisibility(View.GONE);
    tvEmptyState.setText(
        "Search for recipes by ingredient.\n"
            + "Type to see suggestions.");
    tvEmptyState.setVisibility(View.VISIBLE);
    rvSearchResults.setVisibility(View.GONE);
  }

  private void switchToUserMode() {
    if (!isRecipeMode) {
      return;
    }
    isRecipeMode = false;
    tvTabUsers.setBackgroundColor(
        getResources().getColor(R.color.foodtok_green));
    tvTabUsers.setTextColor(
        getResources().getColor(R.color.foodtok_white));
    tvTabRecipes.setBackgroundColor(
        getResources().getColor(R.color.foodtok_divider));
    tvTabRecipes.setTextColor(
        getResources().getColor(R.color.foodtok_text_secondary));

    etSearch.setHint("Search username...");
    chipGroupSelected.setVisibility(View.GONE);
    rvSuggestions.setVisibility(View.GONE);
    rvSearchResults.setVisibility(View.GONE);
    tvEmptyState.setText("Search for users by username.");
    tvEmptyState.setVisibility(View.VISIBLE);
    rvUserResults.setVisibility(View.GONE);
  }

  private void setupSuggestions() {
    suggestionAdapter = new IngredientSuggestionAdapter();
    rvSuggestions.setLayoutManager(
        new LinearLayoutManager(requireContext()));
    rvSuggestions.setAdapter(suggestionAdapter);

    suggestionAdapter.setListener(name -> {
      if (isRecipeMode) {
        addSelectedToken(name);
        etSearch.setText("");
      } else {
        etSearch.setText(name);
        etSearch.setSelection(name.length());
        performUserSearch();
      }
      rvSuggestions.setVisibility(View.GONE);
    });
  }

  private void setupResults() {
    resultAdapter = new GridAdapter(resultRecipes, this::openFeedAt);
    rvSearchResults.setLayoutManager(
        new GridLayoutManager(requireContext(), 2));
    rvSearchResults.setAdapter(resultAdapter);
  }

  private void setupUserResults() {
    userAdapter = new UserSearchAdapter();
    rvUserResults.setLayoutManager(
        new LinearLayoutManager(requireContext()));
    rvUserResults.setAdapter(userAdapter);

    userAdapter.setListener(user -> {
      if (user.id == null) {
        return;
      }
      Fragment profileFragment =
          OtherUserProfileFragment.newInstance(user.id);
      requireActivity().getSupportFragmentManager()
          .beginTransaction()
          .setCustomAnimations(
              R.anim.slide_in_right, R.anim.slide_out_left,
              R.anim.slide_in_left, R.anim.slide_out_right)
          .replace(R.id.fragmentContainer, profileFragment)
          .addToBackStack(null)
          .commit();
      if (getActivity() instanceof MainActivity) {
        ((MainActivity) getActivity())
            .setBottomNavVisibility(false);
      }
    });
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
        Trie activeTrie = isRecipeMode ? searchTrie : usernameTrie;
        List<String> results =
            activeTrie.autocomplete(
                prefix.toLowerCase(), MAX_SUGGESTIONS);
        if (isRecipeMode) {
          results.removeAll(selectedTokens);
        }
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

  /** Fetches all usernames from Supabase and inserts into the username Trie. */
  private void loadUsernames() {
    SupabaseApi api =
        ApiClient.getRestClient().create(SupabaseApi.class);
    api.searchProfiles(
        "not.is.null",
        "username",
        "username.asc"
    ).enqueue(new Callback<List<UserDto>>() {
      @Override
      public void onResponse(Call<List<UserDto>> call,
          Response<List<UserDto>> response) {
        if (response.isSuccessful()
            && response.body() != null) {
          for (UserDto dto : response.body()) {
            if (dto.username != null
                && !dto.username.isEmpty()) {
              usernameTrie.insert(dto.username.toLowerCase());
            }
          }
          Log.d(TAG, "Loaded usernames into Trie; size="
              + usernameTrie.size());
        } else {
          Log.w(TAG, "Failed to load usernames: "
              + response.code());
        }
      }

      @Override
      public void onFailure(Call<List<UserDto>> call,
          Throwable t) {
        Log.w(TAG, "Username load error", t);
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

  /** Routes to the correct search based on the active tab. */
  private void performSearch() {
    if (isRecipeMode) {
      performRecipeSearch();
    } else {
      performUserSearch();
    }
  }

  /** Searches recipes using the selected tokens (tags + ingredients). */
  private void performRecipeSearch() {
    // If there's text in the search box that hasn't been added as a
    // chip yet, treat it as a token so free-text queries still work.
    String raw = etSearch.getText().toString().trim().toLowerCase();
    if (!raw.isEmpty() && !selectedTokens.contains(raw)) {
      addSelectedToken(raw);
    }

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
                  tvEmptyState.setVisibility(View.GONE);
                  rvSearchResults.setVisibility(View.VISIBLE);
                  rvUserResults.setVisibility(View.GONE);
                  resultRecipes.clear();
                  resultRecipes.addAll(recipes);
                  resultAdapter.notifyDataSetChanged();
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

  /** Searches profiles by username prefix via PostgREST ilike. */
  private void performUserSearch() {
    String query = etSearch.getText().toString().trim();
    if (query.isEmpty()) {
      Toast.makeText(requireContext(),
          "Type a username to search",
          Toast.LENGTH_SHORT).show();
      return;
    }

    SupabaseApi api =
        ApiClient.getRestClient().create(SupabaseApi.class);
    api.searchProfiles(
        "ilike.*" + query + "*",
        "id,username,avatar_url",
        "username.asc"
    ).enqueue(new Callback<List<UserDto>>() {
      @Override
      public void onResponse(Call<List<UserDto>> call,
          Response<List<UserDto>> response) {
        if (!isAdded()) {
          return;
        }
        requireActivity().runOnUiThread(() -> {
          if (response.isSuccessful()
              && response.body() != null
              && !response.body().isEmpty()) {
            tvEmptyState.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.GONE);
            rvUserResults.setVisibility(View.VISIBLE);
            userAdapter.setUsers(response.body());
          } else {
            // No exact match — fall back to showing all users
            // in random order as suggestions.
            loadFallbackUsers();
          }
        });
      }

      @Override
      public void onFailure(Call<List<UserDto>> call,
          Throwable t) {
        if (!isAdded()) {
          return;
        }
        requireActivity().runOnUiThread(() ->
            Toast.makeText(requireContext(),
                "Search failed: " + t.getMessage(),
                Toast.LENGTH_SHORT).show());
      }
    });
  }

  /** Fetches all profiles and displays them shuffled as suggestions. */
  private void loadFallbackUsers() {
    SupabaseApi api =
        ApiClient.getRestClient().create(SupabaseApi.class);
    api.searchProfiles(
        "not.is.null",
        "id,username,avatar_url",
        "username.asc"
    ).enqueue(new Callback<List<UserDto>>() {
      @Override
      public void onResponse(Call<List<UserDto>> call,
          Response<List<UserDto>> response) {
        if (!isAdded()) {
          return;
        }
        requireActivity().runOnUiThread(() -> {
          if (response.isSuccessful()
              && response.body() != null
              && !response.body().isEmpty()) {
            List<UserDto> shuffled =
                new ArrayList<>(response.body());
            Collections.shuffle(shuffled);
            tvEmptyState.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.GONE);
            rvUserResults.setVisibility(View.VISIBLE);
            userAdapter.setUsers(shuffled);
          } else {
            rvUserResults.setVisibility(View.GONE);
            tvEmptyState.setText("No users found.");
            tvEmptyState.setVisibility(View.VISIBLE);
          }
        });
      }

      @Override
      public void onFailure(Call<List<UserDto>> call,
          Throwable t) {
        if (!isAdded()) {
          return;
        }
        requireActivity().runOnUiThread(() -> {
          rvUserResults.setVisibility(View.GONE);
          tvEmptyState.setText("No users found.");
          tvEmptyState.setVisibility(View.VISIBLE);
        });
      }
    });
  }
}
