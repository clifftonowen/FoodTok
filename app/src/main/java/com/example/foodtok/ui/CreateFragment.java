package com.example.foodtok.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.foodtok.R;

public class CreateFragment extends Fragment {

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // Check if the user successfully recorded a video and didn't cancel
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Uri recordedVideoUri = result.getData().getData();
                    if (recordedVideoUri != null) {
                        onMediaSelected(recordedVideoUri);
                    }
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                // TODO: pass selected media Uri to the caption/post screen (Step 2)
                onMediaSelected(uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_open_camera).setOnClickListener(v -> openCamera());
        view.findViewById(R.id.btn_open_gallery).setOnClickListener(v -> openGallery());
    }

    // --- Private helpers ---

    private void openCamera() {
        // Launches the device's built-in video camera
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void openGallery() {
        // "video/*" lets the user pick any video from their library
        galleryLauncher.launch("video/*");
    }

    private void onMediaSelected(Uri mediaUri) {
        // 1. Instantiate the destination fragment
        UploadRecipeFragment uploadFragment = new UploadRecipeFragment();

        // 2. Package the URI into a Bundle
        Bundle args = new Bundle();
        args.putString("video_uri", mediaUri.toString());
        uploadFragment.setArguments(args);

        // 3. Execute the fragment transaction
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, uploadFragment)
                .addToBackStack(null) // Allows the user to press 'Back' to select a different video
                .commit();
    }
}
