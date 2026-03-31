package lk.raminsenanayake.thefeast.activity;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.raminsenanayake.thefeast.R;
import lk.raminsenanayake.thefeast.databinding.ActivitySingleProductBinding;
import lk.raminsenanayake.thefeast.databinding.RadioChipLayoutBinding;
import lk.raminsenanayake.thefeast.model.Cart;
import lk.raminsenanayake.thefeast.model.Product;
import lk.raminsenanayake.thefeast.module.GlideApp;

public class SingleProductActivity extends AppCompatActivity {

    private final String TAG = "SingleProductActivity";
    private ActivitySingleProductBinding binding;
    private Product product;
    private int quantity = 1;
    private double total = 0, portionPrice = 0;
    private String portionSize;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        db = FirebaseFirestore.getInstance();

        binding = ActivitySingleProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(binding.singleProductToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.product = getIntent().getSerializableExtra("product", Product.class);
        } else {
            this.product = (Product) getIntent().getSerializableExtra("product");
        }

        binding.plusBtn.setOnClickListener(v -> {
            quantity++;
            binding.quantityTextField.setText(String.valueOf(quantity));
            totalChanged();
        });

        binding.minusBtn.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                binding.quantityTextField.setText(String.valueOf(quantity));
                totalChanged();
            }
        });

        binding.quantityTextField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String enteredQty = s.toString().trim();
                if (enteredQty.isEmpty() || Integer.parseInt(enteredQty) < 1) {
                    quantity = 1;
                    binding.quantityTextField.setText(String.valueOf(quantity));
                } else {
                    quantity = Integer.parseInt(enteredQty);
                }
                totalChanged();
            }
        });

        binding.addToCartBtn.setOnClickListener(v -> {

            Cart.CartItem newCartItem = Cart.CartItem.builder()
                    .productId(product.getProductId())
                    .portionSize(portionSize)
                    .quantity(quantity)
                    .build();

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                DocumentReference dbCart = db.collection("carts")
                        .document(currentUser.getUid());

                dbCart.get().addOnSuccessListener(task -> {
                    if (task.exists()) {
                        Cart cart = task.toObject(Cart.class);
                        boolean matchFound = false;
                        for (Cart.CartItem cartItem : cart.getCartItems()) {

                            if (cartItem.getProductId().equals(product.getProductId())) {
                                matchFound = true;
                                if (cartItem.getPortionSize().equals(portionSize)) {
                                    dbCart.update("cartItems", FieldValue.arrayRemove(cartItem))
                                            .addOnSuccessListener(unused -> {
                                                newCartItem.setQuantity(cartItem.getQuantity() + newCartItem.getQuantity());
                                                dbCart.update("cartItems", FieldValue.arrayUnion(newCartItem))
                                                        .addOnSuccessListener(unused1 -> {
                                                            Snackbar.make(binding.getRoot(), "Updated cart.", Snackbar.LENGTH_SHORT)
                                                                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                                                    .show();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Snackbar.make(binding.getRoot(), "Something went wrong.", Snackbar.LENGTH_SHORT)
                                                                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                                                    .show();
                                                            Log.e(TAG, "Updating cart failed: ", e);
                                                        });
                                            })
                                            .addOnFailureListener(e -> {
                                                Snackbar.make(binding.getRoot(), "Something went wrong.", Snackbar.LENGTH_SHORT)
                                                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                                        .show();
                                                Log.e(TAG, "Couldn't update cart: ", e);
                                            });
                                    break;
                                } else {
                                    dbCart.update("cartItems", FieldValue.arrayUnion(newCartItem))
                                            .addOnSuccessListener(unused -> {
                                                Snackbar.make(binding.getRoot(), "Successfully added to cart.", Snackbar.LENGTH_SHORT)
                                                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                                        .show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Snackbar.make(binding.getRoot(), "Something went wrong.", Snackbar.LENGTH_SHORT)
                                                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                                        .show();
                                                Log.e(TAG, "Adding to cart failed: ", e);
                                            });
                                }
                            }
                        }

                        if (!matchFound) {
                            dbCart.update("cartItems", FieldValue.arrayUnion(newCartItem))
                                    .addOnSuccessListener(unused -> {
                                        Snackbar.make(binding.getRoot(), "Successfully added to cart.", Snackbar.LENGTH_SHORT)
                                                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                                .show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Snackbar.make(binding.getRoot(), "Something went wrong.", Snackbar.LENGTH_SHORT)
                                                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                                .show();
                                        Log.e(TAG, "Adding to cart failed: ", e);
                                    });
                        }

                    } else {
                        Cart cart = Cart.builder()
                                .cartItems(List.of(newCartItem))
                                .build();

                        dbCart.set(cart)
                                .addOnSuccessListener(unused -> {
                                    Snackbar.make(binding.getRoot(), "Successfully added to cart.", Snackbar.LENGTH_SHORT)
                                            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                            .show();
                                })
                                .addOnFailureListener(e -> {
                                    Snackbar.make(binding.getRoot(), "Something went wrong.", Snackbar.LENGTH_SHORT)
                                            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                            .show();
                                    Log.e(TAG, "Adding to cart failed: ", e);
                                });
                    }
                    SharedPreferences sharedPreferences = getBaseContext().getSharedPreferences("cart", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("isSingleProductAdded", true);
                    editor.apply();
                });

            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();

        binding.productName.setText(product.getName());
        binding.productDescription.setText(product.getDescription());

        for (Map.Entry<String, Double> portion : product.getPortionList().entrySet()) {
            Chip chip = RadioChipLayoutBinding
                    .inflate(getLayoutInflater(), binding.portionChipGroup, false).getRoot();
            chip.setText(portion.getKey());
            chip.setTag(portion.getValue());
            binding.portionChipGroup.addView(chip);

            chip.setOnCheckedChangeListener((checkedChip, isChecked) -> {
                if (isChecked) {
                    portionSize = checkedChip.getText().toString();
                    portionPrice = Double.parseDouble(String.valueOf(checkedChip.getTag()));
                    totalChanged();
                }
            });
        }

        ((Chip)binding.portionChipGroup.getChildAt(0)).setChecked(true);

        if (!product.getImage().isEmpty()) {
            StorageReference image = FirebaseStorage.getInstance().getReference("product-images")
                    .child(product.getImage());

            GlideApp.with(getBaseContext())
                    .load(image)
                    .into(binding.productImage);
        } else {
            GlideApp.with(getBaseContext())
                    .load(R.drawable.logo_transparent)
                    .into(binding.productImage);
        }

        binding.quantityTextField.setText(String.valueOf(quantity));
    }

    private void totalChanged() {
        total = portionPrice * quantity;
        binding.productTotalTextView.setText(String.format(Locale.US, "LKR %,.2f", total));
    }
}