package com.example.foodtok.ui;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.foodtok.R;
import com.example.foodtok.models.Ingredient;
import com.example.foodtok.models.Recipe;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * Fragment that lets a user fill in the metadata for a newly recorded/selected
 * video and post it as a Recipe. Receives the video Uri as an argument.
 *
 * Only the title is required — all other fields are optional and can be
 * filled in by the Gemini enrichment service after posting.
 */
public class UploadRecipeFragment extends Fragment {

    private static final String ARG_VIDEO_URI = "video_uri";

    // NumberPicker bounds for prep/cook times
    private static final int TIME_MIN = 0;
    private static final int TIME_MAX = 240; // 4 hours is a reasonable upper bound
    private static final int PREP_TIME_DEFAULT = 10;
    private static final int COOK_TIME_DEFAULT = 20;

    private Uri videoUri;

    private ImageView videoThumbnail;
    private EditText titleInput;
    private EditText tagNameInput;
    private Button addTagButton;
    private LinearLayout tagList;
    private EditText ingredientNameInput;
    private Button addIngredientButton;
    private LinearLayout ingredientList;
    private NumberPicker prepTimePicker;
    private NumberPicker cookTimePicker;
    private EditText caloriesInput;
    private EditText instructionsInput;
    private Button postButton;

    /**
     * Factory method — preferred way to construct this fragment so the video
     * Uri survives configuration changes via the arguments Bundle.
     */
    public static UploadRecipeFragment newInstance(Uri videoUri) {
        UploadRecipeFragment fragment = new UploadRecipeFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_VIDEO_URI, videoUri);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // Retrieve as String, then parse to Uri
            String uriString = getArguments().getString(ARG_VIDEO_URI);
            if (uriString != null) {
                videoUri = Uri.parse(uriString);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_upload_recipe, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupInputFilters();
        setupNumberPickers();
        setupVideoPreview();

        addTagButton.setOnClickListener(v -> addTagRow());
        addIngredientButton.setOnClickListener(v -> addIngredientRow());
        postButton.setOnClickListener(v -> postRecipe());
    }

    private void bindViews(View view) {
        videoThumbnail = view.findViewById(R.id.videoThumbnail);
        titleInput = view.findViewById(R.id.titleInput);
        tagNameInput = view.findViewById(R.id.tagNameInput);
        addTagButton = view.findViewById(R.id.addTagButton);
        tagList = view.findViewById(R.id.tagList);
        ingredientNameInput = view.findViewById(R.id.ingredientNameInput);
        addIngredientButton = view.findViewById(R.id.addIngredientButton);
        ingredientList = view.findViewById(R.id.ingredientList);
        prepTimePicker = view.findViewById(R.id.prepTimePicker);
        cookTimePicker = view.findViewById(R.id.cookTimePicker);
        caloriesInput = view.findViewById(R.id.caloriesInput);
        instructionsInput = view.findViewById(R.id.instructionsInput);
        postButton = view.findViewById(R.id.postButton);
    }

    /**
     * Prevents commas and whitespace from being typed into the tag and
     * ingredient inputs at the character level — much better UX than rejecting
     * the whole entry on submit.
     */
    private void setupInputFilters() {
        InputFilter noCommasOrSpaces = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (c == ',' || Character.isWhitespace(c)) {
                    return ""; // reject the character
                }
            }
            return null; // accept as-is
        };

        tagNameInput.setFilters(new InputFilter[]{noCommasOrSpaces});
        ingredientNameInput.setFilters(new InputFilter[]{noCommasOrSpaces});
    }

    private void setupNumberPickers() {
        prepTimePicker.setMinValue(TIME_MIN);
        prepTimePicker.setMaxValue(TIME_MAX);
        prepTimePicker.setValue(PREP_TIME_DEFAULT);
        prepTimePicker.setWrapSelectorWheel(false);

        cookTimePicker.setMinValue(TIME_MIN);
        cookTimePicker.setMaxValue(TIME_MAX);
        cookTimePicker.setValue(COOK_TIME_DEFAULT);
        cookTimePicker.setWrapSelectorWheel(false);
    }

    private void setupVideoPreview() {
        if (videoUri == null || getContext() == null) {
            return;
        }

        // 1. Setup the native threading components
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        // 2. Push the heavy work to the background thread
        executor.execute(() -> {
            Bitmap thumbnail = null;
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            try {
                // Point the retriever at your video file
                retriever.setDataSource(getContext(), videoUri);

                // Extract a frame at the 1-second mark (1,000,000 microseconds)
                thumbnail = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // finally block to prevent memory leaks
                try {
                    retriever.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // 3. The background work is done. Pass the image back to the Main UI Thread
            final Bitmap finalThumbnail = thumbnail;
            mainHandler.post(() -> {
                if (finalThumbnail != null && videoThumbnail != null) {
                    videoThumbnail.setImageBitmap(finalThumbnail);
                } else {
                    Toast.makeText(getContext(), "Could not load video preview", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Adds a tag row to the dynamic list. The input filter already blocks
     * commas and whitespace, so we only need to check for empty and duplicate.
     */
    private void addTagRow() {
        String name = tagNameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Enter a tag", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rowExists(tagList, name)) {
            Toast.makeText(requireContext(), "Tag already added", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout row = buildRow("#" + name, name);
        tagList.addView(row);
        tagNameInput.setText("");
    }

    /**
     * Adds an ingredient row to the dynamic list. Same duplicate protection
     * as tags. Allergen status isn't tracked per-ingredient — that's handled
     * by the user profile blacklist and Gemini enrichment.
     */
    private void addIngredientRow() {
        String name = ingredientNameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Enter an ingredient name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rowExists(ingredientList, name)) {
            Toast.makeText(requireContext(), "Ingredient already added", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout row = buildRow("• " + name, name);
        ingredientList.addView(row);
        ingredientNameInput.setText("");
    }

    /**
     * Builds a row with a label and a remove button. The underlying data
     * (the raw name) is stored in the row's tag for retrieval at submit time.
     */
    private LinearLayout buildRow(String displayText, String dataValue) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView nameView = new TextView(requireContext());
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameView.setLayoutParams(nameParams);
        nameView.setText(displayText);
        nameView.setTextSize(15);

        Button removeButton = new Button(requireContext());
        removeButton.setText("✕");
        // Need a final reference for the lambda to remove the correct row
        final LinearLayout rowRef = row;
        removeButton.setOnClickListener(v -> {
            ViewGroup parent = (ViewGroup) rowRef.getParent();
            if (parent != null) parent.removeView(rowRef);
        });

        row.addView(nameView);
        row.addView(removeButton);
        row.setTag(dataValue);
        return row;
    }

    /**
     * Checks if a row with the given name (case-insensitive) already exists
     * in the given container.
     */
    private boolean rowExists(LinearLayout container, String name) {
        String lower = name.toLowerCase();
        for (int i = 0; i < container.getChildCount(); i++) {
            Object tag = container.getChildAt(i).getTag();
            if (tag instanceof String && ((String) tag).toLowerCase().equals(lower)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates the form, builds a Recipe from the inputs, and triggers
     * the post action. Only title and video are required — everything else
     * can be left blank and filled in later by the enrichment service.
     */
    private void postRecipe() {
        String title = titleInput.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show();
            titleInput.requestFocus();
            return;
        }
        if (videoUri == null) {
            Toast.makeText(requireContext(), "No video selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build the recipe incrementally
        String id = UUID.randomUUID().toString();
        Recipe recipe = new Recipe(id, videoUri.toString());
        recipe.setTitle(title);

        // Tags — read each row's tag data
        for (int i = 0; i < tagList.getChildCount(); i++) {
            Object tag = tagList.getChildAt(i).getTag();
            if (tag instanceof String) {
                recipe.addTag((String) tag);
            }
        }

        // Ingredients — read each row's tag data
        for (int i = 0; i < ingredientList.getChildCount(); i++) {
            Object tag = ingredientList.getChildAt(i).getTag();
            if (tag instanceof String) {
                // Calories default to 0 — Gemini enrichment estimates the total later
                recipe.addIngredient(new Ingredient((String) tag, 0.0));
            }
        }

        // Numeric fields — NumberPickers always return valid ints
        recipe.setPrepTimeMinutes(prepTimePicker.getValue());
        recipe.setCookTimeMinutes(cookTimePicker.getValue());
        recipe.setEstimatedCalories(parseDoubleSafe(caloriesInput.getText().toString(), 0.0));

        // Instructions stored on the description field for now
        recipe.setDescription(instructionsInput.getText().toString().trim());

        // TODO: send `recipe` to the backend (e.g., RecipeServiceProvider.upload(recipe, ...))
        Toast.makeText(requireContext(),
                "Recipe \"" + recipe.getTitle() + "\" ready to post!",
                Toast.LENGTH_LONG).show();

        // Pop back to the previous screen after posting
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        }
    }

    private double parseDoubleSafe(String s, double fallback) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}