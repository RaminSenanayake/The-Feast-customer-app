package lk.raminsenanayake.thefeast.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.JsonObject;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.raminsenanayake.thefeast.R;
import lk.raminsenanayake.thefeast.api.MessagingAPI;
import lk.raminsenanayake.thefeast.api.StripeAPI;
import lk.raminsenanayake.thefeast.databinding.ActivityCheckoutBinding;
import lk.raminsenanayake.thefeast.model.Cart;
import lk.raminsenanayake.thefeast.model.Order;
import lk.raminsenanayake.thefeast.module.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class CheckoutActivity extends AppCompatActivity {

    private final String TAG = "CheckoutActivity";
    private ActivityCheckoutBinding binding;
    private double orderTotal, total, deliveryFee = 0, deliveryFeePerKm = 100;
    private long deliveryDistance = 0;
    private PaymentSheet paymentSheet;
    private String paymentClientSecret, location, orderType = "Pickup";
    private Cart cart;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        paymentSheet = new PaymentSheet.Builder(this::onPaymentSheetResult).build(this);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        orderTotal = getIntent().getDoubleExtra("total", 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.cart = getIntent().getSerializableExtra("cart", Cart.class);
        } else {
            this.cart = (Cart) getIntent().getSerializableExtra("cart");
        }

        setTotal();

        binding.orderOptions.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == binding.pickup.getId()) {
                deliveryFee = 0;
                orderType = binding.pickup.getText().toString();
                if (deliveryDistance < 5000) {
                    binding.deliveryFeeText.setVisibility(View.INVISIBLE);
                } else {
                    binding.deliveryFeeText.setText(getText(R.string.long_distance_string));
                    binding.deliveryFeeText.setVisibility(View.VISIBLE);
                }
                binding.deliveryFee.setText(String.format(Locale.US, "LKR %,.2f", deliveryFee));
                setTotal();
            } else if (checkedId == binding.delivery.getId()) {
                orderType = binding.delivery.getText().toString();
                Intent intent = new Intent(this, MapsActivity.class);
                locationResultLauncher.launch(intent);
            }
        });

        binding.payNowBtn.setOnClickListener(v -> {
            PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder("The Feast")
                    .build();

            paymentSheet.presentWithPaymentIntent(paymentClientSecret, configuration);
        });
    }

    private final ActivityResultLauncher<Intent> locationResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            deliveryDistance = result.getData().getLongExtra("distance", 0);
            location = result.getData().getStringExtra("location");
            if (deliveryDistance < 5000) {
                deliveryFee = ((double) deliveryDistance / 1000) * deliveryFeePerKm;
                binding.deliveryFeeText.setText(getText(R.string.short_distance_string));
                binding.deliveryFeeText.setVisibility(View.VISIBLE);
                binding.deliveryFee.setText(String.format(Locale.US, "LKR %,.2f", deliveryFee));
                setTotal();
            } else {
                binding.pickup.setChecked(true);
            }
            binding.deliveryFeeText.setVisibility(View.VISIBLE);
        } else if (result.getResultCode() == Activity.RESULT_FIRST_USER) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Permission Required")
                    .setMessage("Permission to access location must be granted to pinpoint delivery location.")
                    .setPositiveButton("Give Permission", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", this.getPackageName(), null));
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("No, thanks", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
            binding.pickup.setChecked(true);
        } else {
            binding.pickup.setChecked(true);
        }
    });

    @Override
    protected void onStart() {
        super.onStart();

        binding.orderTotal.setText(String.format(Locale.US, "LKR %,.2f", orderTotal));
        binding.deliveryFee.setText(String.format(Locale.US, "LKR %,.2f", 0.00));
    }

    @Override
    public boolean onSupportNavigateUp() {
        setResult(Activity.RESULT_CANCELED);
        finish();
        return super.onSupportNavigateUp();
    }

    private void setTotal() {
        total = orderTotal + deliveryFee;
        binding.totalFee.setText(String.format(Locale.US, "LKR %,.2f", total));
        fetchPaymentIntent();
    }

    private void fetchPaymentIntent() {
        Retrofit retrofit = RetrofitClient.getInstance();
        StripeAPI stripeAPI = retrofit.create(StripeAPI.class);
        Call<JsonObject> paymentIntentCall = stripeAPI.createPaymentIntent(total);
        paymentIntentCall.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    paymentClientSecret = response.body().get("clientSecret").getAsString();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "payment intent fetch failed: ", t);
            }
        });
    }

    private void onPaymentSheetResult(final PaymentSheetResult paymentSheetResult) {
        binding.payNowBtn.setEnabled(false);
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {

            List<Order.OrderItem> orderItems = new ArrayList<>();

            for (Cart.CartItem cartItem : cart.getCartItems()) {
                Order.OrderItem orderItem = Order.OrderItem.builder()
                        .productId(cartItem.getProductId())
                        .portionSize(cartItem.getPortionSize())
                        .quantity(cartItem.getQuantity())
                        .build();
                orderItems.add(orderItem);
            }

            Order order = Order.builder()
                    .uid(currentUser.getUid())
                    .orderId("#Order" + System.currentTimeMillis())
                    .total(total)
                    .status(Order.PENDING)
                    .orderItems(orderItems)
                    .orderType(orderType)
                    .location(location)
                    .build();

            FirebaseFirestore.getInstance().collection("orders")
                    .add(order)
                    .addOnSuccessListener(documentReference -> {

                        if (!getIntent().hasExtra("isFromCartFragment")) {
                            FirebaseMessaging.getInstance().getToken()
                                    .addOnSuccessListener(token -> {
                                        Retrofit retrofit = RetrofitClient.getInstance();
                                        MessagingAPI api = retrofit.create(MessagingAPI.class);
                                        api.createOrderCreatedMessage(token).enqueue(new Callback<>() {
                                            @Override
                                            public void onResponse(Call<Void> call, Response<Void> response) {

                                            }

                                            @Override
                                            public void onFailure(Call<Void> call, Throwable t) {

                                            }
                                        });
                                    }).addOnFailureListener(e -> {
                                        Log.e(TAG, "failed to obtain token: ", e);
                                    });

                            new MaterialAlertDialogBuilder(this)
                                    .setTitle("Purchase Successful")
                                    .setMessage("Thank you for choosing to dining with us")
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        setResult(Activity.RESULT_OK);
                                        finish();
                                    })
                                    .show();
                            return;
                        }

                        FirebaseFirestore.getInstance().collection("carts")
                                .document(currentUser.getUid())
                                .set(Cart.builder()
                                        .cartItems(List.of())
                                        .build(), SetOptions.mergeFields("cartItems"))
                                .addOnSuccessListener(unused -> {
                                    FirebaseMessaging.getInstance().getToken()
                                            .addOnSuccessListener(token -> {
                                                Retrofit retrofit = RetrofitClient.getInstance();
                                                MessagingAPI api = retrofit.create(MessagingAPI.class);
                                                api.createOrderCreatedMessage(token).enqueue(new Callback<>() {
                                                    @Override
                                                    public void onResponse(Call<Void> call, Response<Void> response) {

                                                    }

                                                    @Override
                                                    public void onFailure(Call<Void> call, Throwable t) {

                                                    }
                                                });
                                            }).addOnFailureListener(e -> {
                                                Log.e(TAG, "failed to obtain token: ", e);
                                            });

                                    new MaterialAlertDialogBuilder(this)
                                            .setTitle("Purchase Successful")
                                            .setMessage("Thank you for choosing to dining with us")
                                            .setPositiveButton("OK", (dialog, which) -> {
                                                setResult(Activity.RESULT_FIRST_USER + 1);
                                                finish();
                                            })
                                            .show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "cart clearing failed: ", e);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "order creation failed: ", e);
                    });

            Log.d(TAG, "Payment completed");
        } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Log.w(TAG, "Payment canceled!");
        } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
            Throwable error = ((PaymentSheetResult.Failed) paymentSheetResult).getError();
            Log.e(TAG, "Payment failed", error);
        }
        binding.payNowBtn.setEnabled(true);
    }
}
