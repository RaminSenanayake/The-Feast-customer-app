package lk.raminsenanayake.thefeast.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.stripe.android.PaymentConfiguration;

import java.util.HashMap;
import java.util.concurrent.Executors;

import lk.raminsenanayake.thefeast.BuildConfig;
import lk.raminsenanayake.thefeast.R;
import lk.raminsenanayake.thefeast.adapter.MainActivityPageAdapter;
import lk.raminsenanayake.thefeast.databinding.ActivityMainBinding;
import lk.raminsenanayake.thefeast.fragment.CartFragment;
import lk.raminsenanayake.thefeast.fragment.FavouritesFragment;
import lk.raminsenanayake.thefeast.module.GlideApp;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnItemSelectedListener {

    private final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding mainBinding;
    private FirebaseAuth auth;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        PaymentConfiguration.init(getBaseContext(), BuildConfig.STRIPE_PUBLISHABLE_KEY);

        if (currentUser == null) {
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
        } else {
            FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(token -> {
                        FirebaseFirestore.getInstance().collection("users")
                                .document(currentUser.getUid())
                                .set(new HashMap<String, String>() {{
                                    put("fcmToken", token);
                                }}, SetOptions.merge());
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "onCreate: ", e);
                    });
        }

        EdgeToEdge.enable(this);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(mainBinding.main, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(mainBinding.mainSideNavigation, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            MarginLayoutParams mlp = (MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin = bars.left;
            mlp.bottomMargin = bars.bottom;
            mlp.rightMargin = bars.right;
            mlp.topMargin = bars.top;
            v.setLayoutParams(mlp);

            return WindowInsetsCompat.CONSUMED;
        });
        MainActivityPageAdapter mainActivityPageAdapter = new MainActivityPageAdapter(this);
        mainBinding.mainViewPager.setAdapter(mainActivityPageAdapter);
        mainBinding.mainViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                mainBinding.bottomNavigation.getMenu().getItem(position).setChecked(true);
                mainBinding.mainSideNavigation.getMenu().getItem(position).setChecked(true);

                if (position == 2) {
                    SharedPreferences sharedPreferences = getSharedPreferences("favourites", MODE_PRIVATE);
                    boolean isOrderAddedToFavourites = sharedPreferences.getBoolean("isOrderAddedToFavourites", false);
                    if (isOrderAddedToFavourites) {
                        FavouritesFragment favouritesFragment = mainActivityPageAdapter.getFavouritesFragment();
                        if (favouritesFragment != null) {
                            favouritesFragment.loadData();
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("isOrderAddedToFavourites", false);
                            editor.apply();
                        }
                    }
                } else if (position == 3) {
                    SharedPreferences sharedPreferences = getSharedPreferences("cart", MODE_PRIVATE);
                    boolean isSingleProductAdded = sharedPreferences.getBoolean("isSingleProductAdded", false);
                    if (isSingleProductAdded) {
                        CartFragment cartFragment = mainActivityPageAdapter.getCartFragment();
                        if (cartFragment != null) {
                            cartFragment.loadData();
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("isSingleProductAdded", false);
                            editor.apply();
                        }
                    }
                }

            }
        });

        mainBinding.bottomNavigation.setOnItemSelectedListener(this);
        mainBinding.mainSideNavigation.setNavigationItemSelectedListener(this);


        GlideApp.with(this)
                .load(currentUser.getPhotoUrl())
                .circleCrop()
                .into(mainBinding.userPhoto);
        mainBinding.userName.setText(currentUser.getDisplayName());
        mainBinding.userEmail.setText(currentUser.getEmail());

        mainBinding.signOutBtn.setOnClickListener(view -> {
            mainBinding.main.close();
            signOut();
        });
    }

    private boolean wasOffline;

    @Override
    protected void onStart() {
        super.onStart();
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                if (wasOffline) {
                    wasOffline = false;
                    Toast.makeText(MainActivity.this, "You're back online", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                if (!wasOffline) {
                    wasOffline = true;
                    Toast.makeText(MainActivity.this, "You're offline", Toast.LENGTH_SHORT).show();
                }
            }
        };
        cm.registerDefaultNetworkCallback(networkCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.unregisterNetworkCallback(networkCallback);
        networkCallback = null;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int itemId = item.getItemId();
        mainBinding.main.close();

        if (itemId == R.id.home_tab) {
            mainBinding.mainViewPager.setCurrentItem(0);
            return true;
        } else if (itemId == R.id.orders_tab) {
            mainBinding.mainViewPager.setCurrentItem(1);
            return true;
        } else if (itemId == R.id.saved_tab) {
            mainBinding.mainViewPager.setCurrentItem(2);
            return true;
        } else if (itemId == R.id.cart_tab) {
            mainBinding.mainViewPager.setCurrentItem(3);
            return true;
        } else if (itemId == R.id.profile_tab) {
            mainBinding.mainViewPager.setCurrentItem(4);
            return true;
        } else {
            return false;
        }

    }

    public void signOut() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        // Firebase sign out
        auth.signOut();

        // When a user signs out, clear the current user credential state from all credential providers.
        ClearCredentialStateRequest clearRequest = new ClearCredentialStateRequest();
        CredentialManager.create(this).clearCredentialStateAsync(
                clearRequest,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(@NonNull Void result) {
                        Intent intent = new Intent(MainActivity.this, AuthActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        Log.e("GoogleLogin", "Couldn't clear user credentials: " + e.getLocalizedMessage());
                    }
                });
    }
}