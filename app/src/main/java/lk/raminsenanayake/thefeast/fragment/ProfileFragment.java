package lk.raminsenanayake.thefeast.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.badge.ExperimentalBadgeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;

import lk.raminsenanayake.thefeast.activity.MainActivity;
import lk.raminsenanayake.thefeast.databinding.FragmentProfileBinding;
import lk.raminsenanayake.thefeast.module.GlideApp;

public class ProfileFragment extends Fragment {
    private final String TAG = ProfileFragment.class.getSimpleName();
    private FragmentProfileBinding binding;
    private FirebaseUser currentUser;
    private Uri photoUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @OptIn(markerClass = ExperimentalBadgeUtils.class)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "current Photo url: " + currentUser.getPhotoUrl());
        GlideApp.with(view)
                .load(currentUser.getPhotoUrl())
                .circleCrop()
                .into(binding.profileImage);

        String providerId = currentUser.getProviderData().get(1).getProviderId();
        if (providerId.equals("google.com")) {
            binding.profilePasswordLayout.setEnabled(false);
            binding.profilePasswordLayout.setVisibility(View.GONE);
            binding.profileImage.setEnabled(false);
            binding.editIcon.setVisibility(View.GONE);
        } else {
            binding.profilePasswordLayout.setEnabled(true);
            binding.profilePasswordLayout.setVisibility(View.VISIBLE);
            binding.profileImage.setEnabled(true);
            binding.editIcon.setVisibility(View.VISIBLE);
        }

        binding.profileNameField.setText(currentUser.getDisplayName());

        binding.profileImage.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissionsLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                } else {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    profileImageResultLauncher.launch(intent);
                }
            } else {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                profileImageResultLauncher.launch(intent);
            }
        });

        binding.profileUpdateBtn.setOnClickListener(v -> {
            binding.profileUpdateBtn.setEnabled(false);
            String name = binding.profileNameField.getText().toString().trim();
            String password = binding.profilePasswordField.getText().toString().trim();

            if (name.isEmpty()) {
                binding.profileNameLayout.setError("Name is required");
                binding.profileUpdateBtn.setEnabled(true);
                return;
            }

            if (name.length() > 20) {
                binding.profileNameLayout.setError("Name cannot exceed 20 characters");
                binding.profileUpdateBtn.setEnabled(true);
                return;
            }

            UserProfileChangeRequest.Builder updatingProfile = new UserProfileChangeRequest.Builder();
            if (!currentUser.getDisplayName().equals(name)) {
                updatingProfile.setDisplayName(name);
            }

            if (photoUri != null) {
                FirebaseStorage.getInstance().getReference("profile-images")
                        .child(currentUser.getUid())
                        .putFile(photoUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            taskSnapshot.getMetadata().getReference().getDownloadUrl()
                                    .addOnSuccessListener(uri -> {
                                        updatingProfile.setPhotoUri(uri);
                                        currentUser.updateProfile(updatingProfile.build())
                                                .addOnSuccessListener(unused -> {
                                                    Snackbar.make(binding.getRoot(), "Profile updated. Restart app to view changes.", Snackbar.LENGTH_SHORT)
                                                            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                                            .show();
                                                    binding.profileUpdateBtn.setEnabled(true);
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "failed to update profile: ", e);
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "failed to get download url: ", e);
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "failed to upload profile pic: ", e);
                        });
            } else if (!currentUser.getDisplayName().equals(name)) {
                currentUser.updateProfile(updatingProfile.build())
                        .addOnSuccessListener(unused -> {
                            Snackbar.make(binding.getRoot(), "Profile updated. Restart app to view changes.", Snackbar.LENGTH_SHORT)
                                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                    .show();
                            binding.profileUpdateBtn.setEnabled(true);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "failed to update profile: ", e);
                        });
            }

            if (!password.isEmpty()) {
                currentUser.updatePassword(password)
                        .addOnSuccessListener(unused -> {
                            Snackbar.make(binding.getRoot(), "Password updated.", Snackbar.LENGTH_SHORT)
                                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                    .show();
                            ((MainActivity) getActivity()).signOut();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "failed to update password: ", e);
                        });
            }

            if (currentUser.getDisplayName().equals(name) && photoUri == null && password.isEmpty()) {
                Log.i(TAG, "Nothing updated");
                binding.profileUpdateBtn.setEnabled(true);
            }
        });
    }

    private ActivityResultLauncher<Intent> profileImageResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            photoUri = result.getData().getData();

            GlideApp.with(this)
                    .load(photoUri)
                    .circleCrop()
                    .into(binding.profileImage);
        }
    });

    private ActivityResultLauncher<String> requestPermissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        if (result) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            profileImageResultLauncher.launch(intent);
        } else {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Permission Required")
                    .setMessage("To change your profile picture you must grant permission to access your photos.")
                    .setPositiveButton("Give Permission", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", getActivity().getPackageName(), null));
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("No, thanks", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        }
    });


}