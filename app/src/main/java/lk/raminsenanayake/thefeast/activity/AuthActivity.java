package lk.raminsenanayake.thefeast.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.AdditionalUserInfo;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Objects;
import java.util.concurrent.Executors;

import lk.raminsenanayake.thefeast.R;
import lk.raminsenanayake.thefeast.adapter.AuthActivityPageAdapter;
import lk.raminsenanayake.thefeast.api.MessagingAPI;
import lk.raminsenanayake.thefeast.databinding.ActivityAuthBinding;
import lk.raminsenanayake.thefeast.module.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class AuthActivity extends AppCompatActivity {
    private ActivityAuthBinding binding;
    private FirebaseAuth auth;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.authView.setAdapter(new AuthActivityPageAdapter(this));

        new TabLayoutMediator(binding.authTabs, binding.authView, (tab, i) -> {
            switch (i) {
                case 0:
                    tab.setText("Sign In");
                    break;
                case 1:
                    tab.setText("Sign Up");
                    break;
            }
        }).attach();

        binding.googleSignInBtn.setOnClickListener(view -> {
            binding.googleSignInBtn.setEnabled(false);
            GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setAutoSelectEnabled(true)
                    .build();

            launchCredentialManager(googleIdOption);
        });
    }


    int googleSignInCancelCount = 0;

    private void launchCredentialManager(GetGoogleIdOption googleIdOption) {
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();


        credentialManager.getCredentialAsync(
                getBaseContext(),
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        // Extract credential from the result returned by Credential Manager
                        handleSignIn(result.getCredential());
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e("GoogleLogin", "Couldn't retrieve user's credentials: " + e.getLocalizedMessage());

                        if (Objects.equals(e.getMessage(), "User cancelled the selector")) {
                            googleSignInCancelCount++;
                            if (googleSignInCancelCount == 2) {
                                googleSignInCancelCount = 0;
                                runOnUiThread(() -> binding.googleSignInBtn.setEnabled(true));
                            }
                        }
                        if (Objects.equals(e.getMessage(), "No credentials available") || googleSignInCancelCount == 1) {

                            GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(getString(R.string.default_web_client_id))
                                    .setAutoSelectEnabled(true)
                                    .build();

                            launchCredentialManager(googleIdOption);
                        }
                    }
                }
        );
    }

    private void handleSignIn(Credential credential) {
        // Check if credential is of type Google ID
        if (credential instanceof CustomCredential
                && credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            // Create Google ID Token
            Bundle credentialData = credential.getData();
            GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialData);

            // Sign in to Firebase with using the token
            firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
        } else {
            Log.w("GoogleLogin", "Credential is not of type Google ID!");
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("GoogleLogin", "firebaseAuthWithGoogle: successful login");
                        FirebaseMessaging messaging = FirebaseMessaging.getInstance();
                        messaging.deleteToken()
                                .addOnSuccessListener(unused -> {
                                    AdditionalUserInfo additionalUserInfo = task.getResult().getAdditionalUserInfo();
                                    if (additionalUserInfo.isNewUser()) {
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
                                    }
                                    Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }).addOnFailureListener(e -> {
                                    binding.googleSignInBtn.setEnabled(true);
                                    Log.e(this.getClass().getSimpleName(), "failed to delete FCM token: ", e);
                                });
                    } else {
                        // If sign in fails, display a message to the user
                        binding.googleSignInBtn.setEnabled(true);
                        Log.w("GoogleLogin", "signInWithCredential:failure", task.getException());
                    }
                });
    }
}