package com.example.foodtok.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodtok.R;
import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.models.User;
import com.example.foodtok.models.dto.UpdateProfileRequest;
import com.example.foodtok.models.dto.UserDto;
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.services.SupabaseStorageApi;
import com.example.foodtok.util.ApiClient;
import com.example.foodtok.util.Constants;
import com.example.foodtok.util.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Post-signup onboarding screen that collects user preferences in 3 steps:
 * avatar upload from gallery, cuisine/style preferences, and allergen blacklist.
 */
public class OnboardingActivity extends AppCompatActivity {

  private static final String TAG = "OnboardingActivity";
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

  private static final String[] PRESET_AVATARS = {
      "avatar_chef", "avatar_pizza", "avatar_sushi",
      "avatar_cupcake", "avatar_salad", "avatar_burger",
      "avatar_taco", "avatar_ramen", "avatar_cookie"
  };

  private ViewFlipper viewFlipper;
  private TextView tvStepIndicator;
  private Button btnBack;
  private Button btnSkip;
  private Button btnNext;
  private ImageView ivAvatarPreview;
  private ChipGroup chipGroupPreferences;
  private ChipGroup chipGroupAllergens;
  private EditText etCustomPreference;
  private EditText etCustomAllergen;

  private Uri selectedAvatarUri;
  private String selectedPresetAvatar;
  private String uploadedAvatarUrl;
  private final Set<String> selectedPreferences = new HashSet<>();
  private final Set<String> selectedAllergens = new HashSet<>();

  private int currentStep = 0;

  private RecyclerView rvAvatars;

  private final ActivityResultLauncher<String> galleryLauncher =
      registerForActivityResult(
          new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
              selectedAvatarUri = uri;
              selectedPresetAvatar = null;
              ivAvatarPreview.setImageURI(uri);
              if (rvAvatars.getAdapter() != null) {
                rvAvatars.getAdapter().notifyDataSetChanged();
              }
            }
          });

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_onboarding);

    viewFlipper = findViewById(R.id.viewFlipper);
    tvStepIndicator = findViewById(R.id.tvStepIndicator);
    btnBack = findViewById(R.id.btnBack);
    btnSkip = findViewById(R.id.btnSkip);
    btnNext = findViewById(R.id.btnNext);
    ivAvatarPreview = findViewById(R.id.ivAvatarPreview);
    chipGroupPreferences = findViewById(R.id.chipGroupPreferences);
    chipGroupAllergens = findViewById(R.id.chipGroupAllergens);
    etCustomPreference = findViewById(R.id.etCustomPreference);
    etCustomAllergen = findViewById(R.id.etCustomAllergen);

    setupAvatarStep();
    setupChipGroup(
        chipGroupPreferences, PREFERENCE_TAGS, selectedPreferences);
    setupChipGroup(
        chipGroupAllergens, COMMON_ALLERGENS, selectedAllergens);
    setupCustomInput(
        findViewById(R.id.btnAddPreference), etCustomPreference,
        selectedPreferences, chipGroupPreferences);
    setupCustomInput(
        findViewById(R.id.btnAddAllergen), etCustomAllergen,
        selectedAllergens, chipGroupAllergens);

    btnNext.setOnClickListener(v -> onNextClicked());
    btnBack.setOnClickListener(v -> onBackClicked());
    btnSkip.setOnClickListener(v -> onSkipClicked());

    updateStepUi();
  }

  private void setupAvatarStep() {
    Button btnChoose = findViewById(R.id.btnChooseAvatar);
    btnChoose.setOnClickListener(v -> galleryLauncher.launch("image/*"));

    rvAvatars = findViewById(R.id.rvAvatars);
    rvAvatars.setLayoutManager(new GridLayoutManager(this, 3));
    rvAvatars.setAdapter(new PresetAvatarAdapter());
  }

  private void setupChipGroup(ChipGroup group, String[] tags,
      Set<String> selectedSet) {
    for (String tag : tags) {
      Chip chip = new Chip(this);
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

  /**
   * Wires an "Add" button + EditText pair that lets the user type a
   * custom entry and add it as a closable chip to the given ChipGroup.
   */
  private void setupCustomInput(Button btnAdd, EditText editText,
      Set<String> selectedSet, ChipGroup chipGroup) {
    btnAdd.setOnClickListener(v -> {
      String text = editText.getText().toString().trim();
      if (TextUtils.isEmpty(text)) {
        return;
      }
      if (selectedSet.contains(text)) {
        Toast.makeText(this, "Already added", Toast.LENGTH_SHORT)
            .show();
        return;
      }

      selectedSet.add(text);
      addClosableChip(text, selectedSet, chipGroup);
      editText.setText("");
    });
  }

  /**
   * Adds a closable chip to a ChipGroup for a custom entry.
   * Tapping the close icon removes it from both the group and the set.
   */
  private void addClosableChip(String text, Set<String> selectedSet,
      ChipGroup chipGroup) {
    Chip chip = new Chip(this);
    chip.setText(text);
    chip.setCloseIconVisible(true);
    chip.setCheckable(false);
    chip.setChecked(false);
    chip.setOnCloseIconClickListener(v -> {
      selectedSet.remove(text);
      chipGroup.removeView(chip);
    });
    chipGroup.addView(chip);
  }

  private void onNextClicked() {
    if (currentStep < 2) {
      currentStep++;
      viewFlipper.showNext();
      updateStepUi();
    } else {
      finishOnboarding();
    }
  }

  private void onBackClicked() {
    if (currentStep > 0) {
      currentStep--;
      viewFlipper.showPrevious();
      updateStepUi();
    }
  }

  private void onSkipClicked() {
    if (currentStep < 2) {
      currentStep++;
      viewFlipper.showNext();
      updateStepUi();
    } else {
      finishOnboarding();
    }
  }

  private void updateStepUi() {
    tvStepIndicator.setText("Step " + (currentStep + 1) + " of 3");
    btnBack.setVisibility(currentStep > 0 ? View.VISIBLE : View.GONE);
    btnNext.setText(currentStep == 2 ? "Finish" : "Next");
  }

  private void finishOnboarding() {
    btnNext.setEnabled(false);
    btnSkip.setEnabled(false);

    User user = AuthManager.getInstance().getCurrentUser();
    if (user != null) {
      for (String tag : selectedPreferences) {
        user.updateInterestScore(tag, INITIAL_PREFERENCE_SCORE);
      }
      for (String allergen : selectedAllergens) {
        user.addBlacklistedIngredient(allergen);
      }
    }

    if (selectedAvatarUri != null) {
      // User chose an image from gallery — upload it
      uploadAvatarThenSaveProfile();
    } else {
      // Use preset avatar identifier or null
      saveProfileToSupabase(selectedPresetAvatar);
    }
  }

  /**
   * Uploads the selected image to Supabase Storage, then persists
   * the public URL along with preferences to the profiles table.
   */
  private void uploadAvatarThenSaveProfile() {
    String userId = SessionManager.getInstance().getUserId();
    if (userId == null) {
      navigateToMain();
      return;
    }

    byte[] imageBytes;
    try {
      imageBytes = readBytes(selectedAvatarUri);
    } catch (IOException e) {
      Log.w(TAG, "Failed to read avatar image", e);
      saveProfileToSupabase(null);
      return;
    }

    String fileId = UUID.randomUUID().toString();
    String storagePath = userId + "/" + fileId + ".jpg";
    RequestBody body = RequestBody.create(
        MediaType.parse("image/jpeg"), imageBytes);

    SupabaseStorageApi storageApi = ApiClient.getStorageClient()
        .create(SupabaseStorageApi.class);
    storageApi.uploadFile("avatars", storagePath, "image/jpeg", body)
        .enqueue(new Callback<ResponseBody>() {
          @Override
          public void onResponse(Call<ResponseBody> call,
              Response<ResponseBody> response) {
            String avatarUrl = null;
            if (response.isSuccessful()) {
              avatarUrl = Constants.STORAGE_BASE_URL
                  + "object/public/avatars/" + storagePath;
            } else {
              Log.w(TAG, "Avatar upload failed: "
                  + response.code());
            }
            String finalUrl = avatarUrl;
            runOnUiThread(() -> saveProfileToSupabase(finalUrl));
          }

          @Override
          public void onFailure(Call<ResponseBody> call,
              Throwable t) {
            Log.w(TAG, "Avatar upload network error", t);
            runOnUiThread(() -> saveProfileToSupabase(null));
          }
        });
  }

  private void saveProfileToSupabase(String avatarUrl) {
    String userId = SessionManager.getInstance().getUserId();
    if (userId == null) {
      navigateToMain();
      return;
    }

    // Update in-memory user with avatar URL
    User user = AuthManager.getInstance().getCurrentUser();
    if (user != null && avatarUrl != null) {
      user.setAvatarUrl(avatarUrl);
    }

    UpdateProfileRequest request = new UpdateProfileRequest();
    request.avatarUrl = avatarUrl;

    Map<String, Integer> interestMap = new LinkedHashMap<>();
    for (String tag : selectedPreferences) {
      interestMap.put(tag, INITIAL_PREFERENCE_SCORE);
    }
    request.interestProfile = interestMap;
    request.blacklistedIngredients =
        new ArrayList<>(selectedAllergens);

    SupabaseApi api =
        ApiClient.getRestClient().create(SupabaseApi.class);
    api.updateProfile("eq." + userId, request)
        .enqueue(new Callback<List<UserDto>>() {
          @Override
          public void onResponse(Call<List<UserDto>> call,
              Response<List<UserDto>> response) {
            if (!response.isSuccessful()) {
              Log.w(TAG, "Profile update failed: "
                  + response.code());
            }
            runOnUiThread(() -> navigateToMain());
          }

          @Override
          public void onFailure(Call<List<UserDto>> call,
              Throwable t) {
            Log.w(TAG, "Profile update network error", t);
            runOnUiThread(() -> navigateToMain());
          }
        });
  }

  private void navigateToMain() {
    Intent intent = new Intent(this, MainActivity.class);
    intent.setFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
  }

  /** Reads all bytes from a content:// Uri. */
  private byte[] readBytes(Uri uri) throws IOException {
    try (InputStream is = getContentResolver().openInputStream(uri)) {
      if (is == null) {
        throw new IOException("Cannot open input stream for " + uri);
      }
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      byte[] chunk = new byte[8192];
      int bytesRead;
      while ((bytesRead = is.read(chunk)) != -1) {
        buffer.write(chunk, 0, bytesRead);
      }
      return buffer.toByteArray();
    }
  }

  // ── Preset avatar adapter ────────────────────────────────────────────

  private static String emojiForAvatar(String avatar) {
    switch (avatar) {
      case "avatar_chef": return "\uD83D\uDC68\u200D\uD83C\uDF73";
      case "avatar_pizza": return "\uD83C\uDF55";
      case "avatar_sushi": return "\uD83C\uDF63";
      case "avatar_cupcake": return "\uD83E\uDDC1";
      case "avatar_salad": return "\uD83E\uDD57";
      case "avatar_burger": return "\uD83C\uDF54";
      case "avatar_taco": return "\uD83C\uDF2E";
      case "avatar_ramen": return "\uD83C\uDF5C";
      case "avatar_cookie": return "\uD83C\uDF6A";
      default: return "\uD83C\uDF7D";
    }
  }

  /**
   * RecyclerView adapter for the preset avatar emoji grid.
   * Selecting a preset clears any gallery image selection.
   */
  private class PresetAvatarAdapter
      extends RecyclerView.Adapter<PresetAvatarAdapter.VH> {

    @Override
    public VH onCreateViewHolder(
        android.view.ViewGroup parent, int viewType) {
      TextView tv = new TextView(parent.getContext());
      int size = (int) (80 * parent.getContext().getResources()
          .getDisplayMetrics().density);
      tv.setLayoutParams(new RecyclerView.LayoutParams(
          RecyclerView.LayoutParams.MATCH_PARENT, size));
      tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
      tv.setGravity(android.view.Gravity.CENTER);
      tv.setTextSize(32);
      return new VH(tv);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
      String avatar = PRESET_AVATARS[position];
      holder.tv.setText(emojiForAvatar(avatar));

      boolean selected = avatar.equals(selectedPresetAvatar)
          && selectedAvatarUri == null;
      if (selected) {
        holder.tv.setBackgroundResource(R.drawable.bg_chip_selected);
        holder.tv.setAlpha(1.0f);
      } else {
        holder.tv.setBackgroundResource(R.drawable.bg_chip_unselected);
        holder.tv.setAlpha(0.7f);
      }

      holder.tv.setOnClickListener(v -> {
        selectedPresetAvatar = avatar;
        selectedAvatarUri = null;
        ivAvatarPreview.setImageResource(
            R.drawable.ic_profile_placeholder);
        notifyDataSetChanged();
      });
    }

    @Override
    public int getItemCount() {
      return PRESET_AVATARS.length;
    }

    class VH extends RecyclerView.ViewHolder {
      final TextView tv;

      VH(TextView tv) {
        super(tv);
        this.tv = tv;
      }
    }
  }
}