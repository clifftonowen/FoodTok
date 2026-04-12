package com.example.foodtok.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.foodtok.R;
import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.models.User;
import com.example.foodtok.models.dto.UpdateProfileRequest;
import com.example.foodtok.models.dto.UserDto;
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.util.ApiClient;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManagePreferencesFragment extends Fragment {

    private static final int INITIAL_PREFERENCE_SCORE = 30;

    private static final String[] PREFERENCE_TAGS = {
            "Italian", "Vegan", "Quick Meals", "Desserts", "Asian",
            "Healthy", "Comfort Food", "Seafood", "Spicy", "Breakfast",
            "Mexican", "Mediterranean", "Baking", "Grilling", "Soup"
    };

    private static final String[] COMMON_ALLERGENS = {
            "Peanuts", "Gluten", "Dairy", "Shellfish", "Eggs",
            "Soy", "Tree Nuts", "Fish", "Sesame", "Wheat"
    };

    private TextView btnBack;
    private Button btnSave;
    private ChipGroup chipGroupPreferences;
    private ChipGroup chipGroupAllergens;
    private EditText etCustomPreference;
    private EditText etCustomAllergen;

    private final Set<String> selectedPreferences = new HashSet<>();
    private final Set<String> selectedAllergens = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_preferences, container, false);

        btnBack = view.findViewById(R.id.btnBack);
        btnSave = view.findViewById(R.id.btnSave);
        chipGroupPreferences = view.findViewById(R.id.chipGroupPreferences);
        chipGroupAllergens = view.findViewById(R.id.chipGroupAllergens);
        etCustomPreference = view.findViewById(R.id.etCustomPreference);
        etCustomAllergen = view.findViewById(R.id.etCustomAllergen);

        setupChipGroup(chipGroupPreferences, PREFERENCE_TAGS, selectedPreferences);
        setupChipGroup(chipGroupAllergens, COMMON_ALLERGENS, selectedAllergens);

        setupCustomInput(
                view.findViewById(R.id.btnAddPreference),
                etCustomPreference,
                selectedPreferences,
                chipGroupPreferences
        );

        setupCustomInput(
                view.findViewById(R.id.btnAddAllergen),
                etCustomAllergen,
                selectedAllergens,
                chipGroupAllergens
        );

        btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        btnSave.setOnClickListener(v -> savePreferences());

        loadCurrentPreferences();

        return view;
    }

    private void setupChipGroup(ChipGroup group, String[] tags, Set<String> selectedSet) {
        for (String tag : tags) {
            Chip chip = new Chip(requireContext());
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedSet.add(tag);
                } else {
                    selectedSet.remove(tag);
                }
            });
            group.addView(chip);
        }
    }

    private void setupCustomInput(Button btnAdd,
                                  EditText editText,
                                  Set<String> selectedSet,
                                  ChipGroup chipGroup) {
        btnAdd.setOnClickListener(v -> {
            String text = editText.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                return;
            }

            if (selectedSet.contains(text)) {
                Toast.makeText(requireContext(), "Already added", Toast.LENGTH_SHORT).show();
                return;
            }

            selectedSet.add(text);
            addClosableChip(text, selectedSet, chipGroup, true);
            editText.setText("");
        });
    }

    private void addClosableChip(String text,
                                 Set<String> selectedSet,
                                 ChipGroup chipGroup,
                                 boolean customChip) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);

        if (customChip) {
            chip.setCloseIconVisible(true);
            chip.setCheckable(false);
            chip.setChecked(false);
            chip.setOnCloseIconClickListener(v -> {
                selectedSet.remove(text);
                chipGroup.removeView(chip);
            });
        } else {
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            chip.setChecked(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedSet.add(text);
                } else {
                    selectedSet.remove(text);
                }
            });
        }

        chipGroup.addView(chip);
    }

    private void loadCurrentPreferences() {
        String userId = AuthManager.getInstance().getCurrentUser().getId();

        ApiClient.getSupabaseApi()
                .getProfiles("eq." + userId,
                        "id,interest_profile,blacklisted_ingredients")
                .enqueue(new Callback<List<UserDto>>() {
                    @Override
                    public void onResponse(Call<List<UserDto>> call,
                                           Response<List<UserDto>> response) {
                        if (!isAdded() || !response.isSuccessful()
                                || response.body() == null
                                || response.body().isEmpty()) {
                            return;
                        }

                        UserDto user = response.body().get(0);

                        if (user.interestProfile != null) {
                            for (String key : user.interestProfile.keySet()) {
                                selectedPreferences.add(key);
                            }
                        }

                        if (user.blacklistedIngredients != null) {
                            selectedAllergens.addAll(user.blacklistedIngredients);
                        }

                        preselectLoadedData();
                    }

                    @Override
                    public void onFailure(Call<List<UserDto>> call, Throwable t) {
                    }
                });
    }

    private void preselectLoadedData() {
        for (int i = 0; i < chipGroupPreferences.getChildCount(); i++) {
            View child = chipGroupPreferences.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (selectedPreferences.contains(chip.getText().toString())) {
                    chip.setChecked(true);
                }
            }
        }

        for (int i = 0; i < chipGroupAllergens.getChildCount(); i++) {
            View child = chipGroupAllergens.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (selectedAllergens.contains(chip.getText().toString())) {
                    chip.setChecked(true);
                }
            }
        }

        for (String pref : new HashSet<>(selectedPreferences)) {
            if (!isInDefaultList(pref, PREFERENCE_TAGS)) {
                addClosableChip(pref, selectedPreferences, chipGroupPreferences, true);
            }
        }

        for (String allergen : new HashSet<>(selectedAllergens)) {
            if (!isInDefaultList(allergen, COMMON_ALLERGENS)) {
                addClosableChip(allergen, selectedAllergens, chipGroupAllergens, true);
            }
        }
    }

    private boolean isInDefaultList(String value, String[] defaults) {
        for (String item : defaults) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private void savePreferences() {
        String userId = AuthManager.getInstance().getCurrentUser().getId();

        UpdateProfileRequest request = new UpdateProfileRequest();

        Map<String, Integer> interestMap = new LinkedHashMap<>();
        for (String tag : selectedPreferences) {
            interestMap.put(tag, INITIAL_PREFERENCE_SCORE);
        }

        request.interestProfile = interestMap;
        request.blacklistedIngredients = new ArrayList<>(selectedAllergens);

        ApiClient.getSupabaseApi()
                .updateProfile("eq." + userId, request)
                .enqueue(new Callback<List<UserDto>>() {
                    @Override
                    public void onResponse(Call<List<UserDto>> call,
                                           Response<List<UserDto>> response) {
                        if (!isAdded()) {
                            return;
                        }

                        if (response.isSuccessful()) {
                            User user = AuthManager.getInstance().getCurrentUser();
                            if (user != null) {
                                for (String tag : selectedPreferences) {
                                    user.updateInterestScore(tag, INITIAL_PREFERENCE_SCORE);
                                }
                                for (String allergen : selectedAllergens) {
                                    user.addBlacklistedIngredient(allergen);
                                }
                            }

                            Toast.makeText(requireContext(),
                                    "Preferences updated",
                                    Toast.LENGTH_SHORT).show();
                            requireActivity().getSupportFragmentManager().popBackStack();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Failed to update preferences",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<UserDto>> call, Throwable t) {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(requireContext(),
                                "Network error",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}