package lk.raminsenanayake.thefeast.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

import lk.raminsenanayake.thefeast.activity.CheckoutActivity;
import lk.raminsenanayake.thefeast.adapter.CartAdapter;
import lk.raminsenanayake.thefeast.databinding.FavouriteNameInputBinding;
import lk.raminsenanayake.thefeast.databinding.FragmentCartBinding;
import lk.raminsenanayake.thefeast.listener.ShakeListener;
import lk.raminsenanayake.thefeast.model.Cart;
import lk.raminsenanayake.thefeast.model.Favourite;

public class CartFragment extends Fragment {
    private final String TAG = "CartFragment";
    private FragmentCartBinding binding;
    private FirebaseUser currentUser;
    private double cartTotal;
    private ShakeListener shakeListener;
    private BottomSheetBehavior<FrameLayout> saveOrderBottomSheetBehavior;
    private Cart cart;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shakeListener = new ShakeListener(this.getContext());
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        loadData();

        saveOrderBottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet);
        saveOrderBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        binding.proceedBtn.setOnClickListener(v -> {
            saveOrderBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        binding.cancelSaveOrderBtn.setOnClickListener(v -> {
            saveOrderBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            goToCheckout();
        });
        binding.saveOrderBtn.setOnClickListener(v -> {
            binding.proceedBtn.setEnabled(false);
            saveOrderBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            FavouriteNameInputBinding textViewBinding = FavouriteNameInputBinding.inflate(getLayoutInflater());
            AlertDialog alertDialog = new MaterialAlertDialogBuilder(getContext())
                    .setTitle("Favourites Title")
                    .setMessage("Enter a name for this favourites order.")
                    .setView(textViewBinding.getRoot())
                    .setPositiveButton("Save", null)
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        binding.proceedBtn.setEnabled(true);
                        dialog.dismiss();
                    })
                    .create();

            alertDialog.setOnShowListener(dialog -> {
                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
                    String favouriteName = textViewBinding.favouriteNameField.getText().toString().trim();

                    textViewBinding.favouriteNameField.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void afterTextChanged(Editable s) {
                            textViewBinding.favouriteNameLayout.setError(null);
                        }

                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }
                    });

                    if (favouriteName.isEmpty()) {
                        textViewBinding.favouriteNameLayout.setError("Name cannot be empty");
                        return;
                    }

                    Favourite favourite = new Favourite(favouriteName, cartTotal);
                    favourite.setCartItems(cart.getCartItems());

                    CollectionReference favouritesCollection = FirebaseFirestore.getInstance()
                            .collection("users").document(currentUser.getUid())
                            .collection("favourites");

                    favouritesCollection.whereEqualTo("name", favouriteName)
                            .get()
                            .addOnSuccessListener(qds -> {
                                if (qds.isEmpty()) {
                                    favouritesCollection.add(favourite)
                                            .addOnSuccessListener(documentReference -> {
                                                SharedPreferences sharedPreferences = getContext().getSharedPreferences("favourites", Context.MODE_PRIVATE);
                                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                                editor.putBoolean("isOrderAddedToFavourites", true);
                                                editor.apply();
                                                goToCheckout();
                                                dialog.dismiss();
                                            })
                                            .addOnFailureListener(e -> {
                                                binding.proceedBtn.setEnabled(true);
                                                Log.e(TAG, "failed to add to favourites: ", e);
                                            });
                                } else {
                                    textViewBinding.favouriteNameLayout.setError("A favourite order already exists with this name");
                                }
                            });
                });
            });
            alertDialog.show();
        });
    }

    private void goToCheckout() {
        Intent intent = new Intent(this.getContext(), CheckoutActivity.class);
        intent.putExtra("total", cartTotal);
        intent.putExtra("cart", cart);
        intent.putExtra("isFromCartFragment", true);
        checkoutLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> checkoutLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_FIRST_USER + 1) {
            ((CartAdapter) binding.cartItemRecyclerView.getAdapter()).clearCart();
        } else {
            binding.proceedBtn.setEnabled(true);
        }
    });


    @Override
    public void onResume() {
        super.onResume();
        shakeListener.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        shakeListener.pause();
    }

    public void loadData() {
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("carts")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(ds -> {
                        if (ds.exists()) {
                            cart = ds.toObject(Cart.class);

                            if (cart.getCartItems().isEmpty()) {
                                binding.cartItemRecyclerView.setVisibility(View.GONE);
                                binding.cartTotalView.setVisibility(View.GONE);
                                binding.cartEmptyView.setVisibility(View.VISIBLE);
                            } else {
                                binding.cartItemRecyclerView.setVisibility(View.VISIBLE);
                                binding.cartTotalView.setVisibility(View.VISIBLE);
                                binding.cartEmptyView.setVisibility(View.GONE);
                            }

                            binding.cartItemRecyclerView.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));
                            binding.cartItemRecyclerView.setAdapter(new CartAdapter(cart.getCartItems(), shakeListener, new CartAdapter.CartItemListener() {
                                @Override
                                public void onTotalChange(double total) {
                                    cartTotal = total;
                                    binding.cartTotal.setText(String.format(Locale.US, "LKR %,.2f", cartTotal));
                                }

                                @Override
                                public void onItemListEmpty() {
                                    binding.proceedBtn.setEnabled(false);
                                    if (saveOrderBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                                        saveOrderBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                                    }
                                    binding.cartItemRecyclerView.setVisibility(View.GONE);
                                    binding.cartTotalView.setVisibility(View.GONE);
                                    binding.cartEmptyView.setVisibility(View.VISIBLE);
                                }
                            }));

                            if (!cart.getCartItems().isEmpty()) {
                                binding.proceedBtn.setEnabled(true);
                            }
                        } else {
                            binding.cartItemRecyclerView.setVisibility(View.GONE);
                            binding.cartTotalView.setVisibility(View.GONE);
                            binding.cartEmptyView.setVisibility(View.VISIBLE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load cart: ", e);
                    });
        }
    }
}