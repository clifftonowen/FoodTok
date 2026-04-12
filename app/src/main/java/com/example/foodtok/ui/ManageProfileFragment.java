package com.example.foodtok.ui;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.foodtok.R;
import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.models.dto.UpdateProfileRequest;
import com.example.foodtok.models.dto.UserDto;
import com.example.foodtok.services.SupabaseStorageApi;
import com.example.foodtok.util.ApiClient;
import com.example.foodtok.util.Constants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageProfileFragment extends Fragment {

    private ImageView ivAvatar;
    private TextView tvChangePhoto;
    private EditText etBio;
    private Button btnSave;

    private Uri pendingAvatarUri = null;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                pendingAvatarUri = uri;
                Glide.with(this).load(uri).circleCrop().into(ivAvatar);
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_profile, container, false);

        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvChangePhoto = view.findViewById(R.id.tvChangePhoto);
        etBio = view.findViewById(R.id.etBio);
        btnSave = view.findViewById(R.id.btnSave);
        TextView btnBack = view.findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        tvChangePhoto.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnSave.setOnClickListener(v -> saveProfile());

        loadCurrentProfile();

        return view;
    }

    private void loadCurrentProfile() {
        String userId = AuthManager.getInstance().getCurrentUser().getId();
        ApiClient.getSupabaseApi()
                .getProfiles("eq." + userId, "id,avatar_url,bio")
                .enqueue(new Callback<List<UserDto>>() {
                    @Override
                    public void onResponse(Call<List<UserDto>> call, Response<List<UserDto>> response) {
                        if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) return;
                        UserDto profile = response.body().get(0);

                        if (!TextUtils.isEmpty(profile.avatarUrl)) {
                            Glide.with(ManageProfileFragment.this)
                                    .load(profile.avatarUrl)
                                    .circleCrop()
                                    .transition(DrawableTransitionOptions.withCrossFade(300))
                                    .into(ivAvatar);
                        }

                        if (!TextUtils.isEmpty(profile.bio)) {
                            etBio.setText(profile.bio);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<UserDto>> call, Throwable t) {}
                });
    }

    private void saveProfile() {
        String bio = etBio.getText().toString().trim();
        if (pendingAvatarUri != null) {
            uploadThenSave(bio);
        } else {
            patchProfile(null, bio);
        }
    }

    private void uploadThenSave(String bio) {
        String userId = AuthManager.getInstance().getCurrentUser().getId();
        byte[] bytes;
        try {
            bytes = readBytes(pendingAvatarUri);
        } catch (IOException e) {
            return;
        }

        String storagePath = userId + "/" + UUID.randomUUID() + ".jpg";
        RequestBody body = RequestBody.create(MediaType.parse("image/jpeg"), bytes);

        ApiClient.getStorageClient()
                .create(SupabaseStorageApi.class)
                .uploadFile("avatars", storagePath, "image/jpeg", body)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful()) return;
                        String avatarUrl = Constants.SUPABASE_URL
                                + "/storage/v1/object/public/avatars/" + storagePath;
                        patchProfile(avatarUrl, bio);
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {}
                });
    }

    private void patchProfile(String avatarUrl, String bio) {
        String userId = AuthManager.getInstance().getCurrentUser().getId();
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.avatarUrl = avatarUrl;
        req.bio = bio;

        ApiClient.getSupabaseApi()
                .updateProfile("eq." + userId, req)
                .enqueue(new Callback<List<UserDto>>() {
                    @Override
                    public void onResponse(Call<List<UserDto>> call, Response<List<UserDto>> response) {
                        requireActivity().runOnUiThread(() ->
                                requireActivity().getSupportFragmentManager().popBackStack());
                    }

                    @Override
                    public void onFailure(Call<List<UserDto>> call, Throwable t) {}
                });
    }

    private byte[] readBytes(Uri uri) throws IOException {
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            return buffer.toByteArray();
        }
    }
}
