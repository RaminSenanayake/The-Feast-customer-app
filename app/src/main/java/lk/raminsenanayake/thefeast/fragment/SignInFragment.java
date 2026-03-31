package lk.raminsenanayake.thefeast.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.messaging.FirebaseMessaging;

import lk.raminsenanayake.thefeast.activity.MainActivity;
import lk.raminsenanayake.thefeast.databinding.FragmentSignInBinding;

public class SignInFragment extends Fragment {

    private FragmentSignInBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSignInBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listenersInit();

        binding.signInBtn.setOnClickListener(view1 -> {
            binding.signInBtn.setEnabled(false);
            String email = binding.signInEmailField.getText().toString().trim();
            String password = binding.signInPasswordField.getText().toString().trim();

            if (validateInput(email, password)) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseMessaging.getInstance().deleteToken()
                                        .addOnSuccessListener(unused -> {
                                            Intent intent = new Intent(view.getContext(), MainActivity.class);
                                            startActivity(intent);
                                            getActivity().finish();
                                        }).addOnFailureListener(e -> {
                                            Log.e(this.getClass().getSimpleName(), "failed to delete FCM token: ", e);
                                            binding.signInBtn.setEnabled(true);
                                        });
                            } else {
                                Log.e(SignInFragment.class.getSimpleName(), "Sign in failed: ", task.getException());
                                if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                    Snackbar.make(view, "User cannot be found. Try creating an account first.", Snackbar.LENGTH_SHORT)
                                            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                            .show();
                                    binding.signInBtn.setEnabled(true);
                                } else {
                                    Snackbar.make(view, "Authentication failed. Try again later.", Snackbar.LENGTH_SHORT)
                                            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                            .show();
                                    binding.signInBtn.setEnabled(true);
                                }
                            }
                        });
            }
        });
    }

    private boolean validateInput(String email, String password) {

        boolean isValidated = true;

        if (email.isEmpty()) {
            binding.signInEmailLayout.setError("Email cannot be empty");
            binding.signInEmailLayout.requestFocus();
            isValidated = false;
        }

        if (password.isEmpty()) {
            binding.signInPasswordLayout.setError("Password cannot be empty");
            binding.signInPasswordLayout.requestFocus();
            isValidated = false;
        }

        return isValidated;
    }

    private void listenersInit() {
        binding.signInEmailField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {

            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.signInEmailLayout.setError(null);
            }
        });

        binding.signInPasswordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {

            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.signInPasswordLayout.setError(null);
            }
        });
    }
}