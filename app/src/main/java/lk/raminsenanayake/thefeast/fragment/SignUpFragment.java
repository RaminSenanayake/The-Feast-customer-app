package lk.raminsenanayake.thefeast.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.messaging.FirebaseMessaging;

import lk.raminsenanayake.thefeast.activity.MainActivity;
import lk.raminsenanayake.thefeast.api.MessagingAPI;
import lk.raminsenanayake.thefeast.databinding.FragmentSignUpBinding;
import lk.raminsenanayake.thefeast.module.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SignUpFragment extends Fragment {
    private final String TAG = "SignUp";
    private FragmentSignUpBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSignUpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        listenersInit();

        binding.signUpBtn.setOnClickListener(view1 -> {
            binding.signUpBtn.setEnabled(false);
            String name = binding.signUpNameField.getText().toString().trim();
            String email = binding.signUpEmailField.getText().toString().trim();
            String password = binding.signUpPasswordField.getText().toString().trim();

            if (validateInput(name, email, password)) {
                FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(task -> {
                            task.getUser().updateProfile(
                                    new UserProfileChangeRequest.Builder()
                                            .setDisplayName(name)
                                            .setPhotoUri(Uri.parse("https://ui-avatars.com/api/?name=" + name.replaceAll(" ", "+")))
                                            .build()
                            ).addOnSuccessListener(unused -> {
                                FirebaseMessaging messaging = FirebaseMessaging.getInstance();
                                messaging.deleteToken()
                                        .addOnSuccessListener(unused1 -> {
                                            messaging.getToken().addOnSuccessListener(s -> {
                                                        Retrofit retrofit = RetrofitClient.getInstance();
                                                        MessagingAPI messagingAPI = retrofit.create(MessagingAPI.class);
                                                        messagingAPI.createWelcomeMessage(s).enqueue(new Callback<Void>() {
                                                            @Override
                                                            public void onResponse(Call<Void> call, Response<Void> response) {

                                                            }

                                                            @Override
                                                            public void onFailure(Call<Void> call, Throwable t) {

                                                            }
                                                        });
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(this.getClass().getSimpleName(), "failed to get FCM token: ", e);
                                                    });
                                        }).addOnFailureListener(e -> {
                                            Log.e(this.getClass().getSimpleName(), "failed to delete FCM token: ", e);
                                            binding.signUpBtn.setEnabled(true);
                                        });
                                Intent intent = new Intent(view.getContext(), MainActivity.class);
                                startActivity(intent);
                                getActivity().finish();
                            }).addOnFailureListener(e -> {
                                Log.e(TAG, "failed to update profile on sign up: ", e);
                                task.getUser().delete();
                                Snackbar.make(view, "Failed to create profile. Try again.", Snackbar.LENGTH_SHORT)
                                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                        .show();
                                binding.signUpBtn.setEnabled(true);
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Sign Up failed", e);
                            if (e instanceof FirebaseAuthUserCollisionException) {
                                Snackbar.make(view, e.getMessage(), Snackbar.LENGTH_SHORT)
                                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                        .show();
                                binding.signUpBtn.setEnabled(true);
                            } else {
                                Snackbar.make(view, "Something went wrong. Try again later.", Snackbar.LENGTH_SHORT)
                                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                        .show();
                                binding.signUpBtn.setEnabled(true);
                            }
                        });
            }

        });

    }

    private boolean validateInput(String name, String email, String password) {

        boolean isValidated = true;

        if (name.isEmpty()) {
            binding.signUpNameLayout.setError("Name cannot be empty");
            binding.signUpNameLayout.requestFocus();
            isValidated = false;
        } else if (name.length() > 20) {
            binding.signUpNameLayout.setError("Name cannot exceed 20 characters");
            binding.signUpNameLayout.requestFocus();
            isValidated = false;
        }

        if (email.isEmpty()) {
            binding.signUpEmailLayout.setError("Email cannot be empty");
            binding.signUpEmailLayout.requestFocus();
            isValidated = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.signUpEmailLayout.setError("Enter valid email");
            binding.signUpEmailLayout.requestFocus();
            isValidated = false;
        }

        if (password.isEmpty()) {
            binding.signUpPasswordLayout.setError("Password cannot be empty");
            binding.signUpPasswordLayout.requestFocus();
            isValidated = false;
        }

        return isValidated;
    }

    private void listenersInit() {
        binding.signUpNameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {

            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.signUpNameLayout.setError(null);
            }
        });

        binding.signUpEmailField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {

            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.signUpEmailLayout.setError(null);
            }
        });

        binding.signUpPasswordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {

            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.signUpPasswordLayout.setError(null);
            }
        });
    }
}