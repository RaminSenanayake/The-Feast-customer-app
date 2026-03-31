package lk.raminsenanayake.thefeast.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import lk.raminsenanayake.thefeast.R;
import lk.raminsenanayake.thefeast.databinding.CartItemCardBinding;
import lk.raminsenanayake.thefeast.listener.ShakeListener;
import lk.raminsenanayake.thefeast.model.Cart;
import lk.raminsenanayake.thefeast.model.Product;
import lk.raminsenanayake.thefeast.module.GlideApp;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private final String TAG = "CartAdapter";
    private List<Cart.CartItem> cartItems;
    private CartItemCardBinding binding;
    private double[] cartItemTotals;
    private CartItemListener listener;
    private DocumentReference dbCart;
    private ShakeListener shakeListener;

    public CartAdapter(List<Cart.CartItem> cartItems, ShakeListener shakeListener, CartItemListener cartItemListener) {
        this.cartItems = cartItems;
        this.listener = cartItemListener;
        this.shakeListener = shakeListener;
        cartItemTotals = new double[cartItems.size()];
        dbCart = FirebaseFirestore.getInstance().collection("carts")
                .document(
                        FirebaseAuth.getInstance().getUid()
                );
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        binding = CartItemCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        shakeListener.setOnShakeListener(() -> {
            if (!cartItems.isEmpty()) {
                dbCart.set(Cart.builder()
                                .cartItems(List.of())
                                .build(), SetOptions.mergeFields("cartItems"))
                        .addOnSuccessListener(unused -> {
                            clearCart();
                            Snackbar.make(parent.getRootView(), "Cart cleared", Snackbar.LENGTH_SHORT)
                                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                    .show();
                        });
            }
        });
        return new ViewHolder(binding.getRoot());
    }

    public void clearCart() {
        cartItems.clear();
        notifyDataSetChanged();
        listener.onItemListEmpty();
        cartItemTotals = new double[0];
        setTotal();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cart.CartItem cartItem = cartItems.get(position);
        AtomicReference<Double> portionPrice = new AtomicReference<>((double) 0);

        FirebaseFirestore.getInstance().collection("products")
                .whereEqualTo("productId", cartItem.getProductId())
                .get()
                .addOnSuccessListener(qds -> {
                    Product product = qds.toObjects(Product.class).get(0);

                    holder.cartItemName.setText(product.getName());

                    for (Map.Entry<String, Double> portion : product.getPortionList().entrySet()) {
                        if (portion.getKey().equals(cartItem.getPortionSize())) {
                            portionPrice.set(portion.getValue());
                            cartItemTotalChanged(cartItem, portionPrice.get(), holder, position);
                            break;
                        }
                    }

                    if (!product.getImage().isEmpty()) {
                        StorageReference image = FirebaseStorage.getInstance().getReference("product-images")
                                .child(product.getImage());

                        GlideApp.with(holder.itemView)
                                .load(image)
                                .into(holder.cartItemImage);
                    } else {
                        GlideApp.with(holder.itemView)
                                .load(R.drawable.logo_transparent)
                                .into(holder.cartItemImage);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to obtain product id: ", e);
                });

        holder.cartItemPortion.setText(cartItem.getPortionSize());

//        cartItemTotalChanged(cartItem,portionPrice.get(), holder, position);

        holder.cartItemQtyAddBtn.setOnClickListener(v -> {
            holder.cartItemQtyAddBtn.setClickable(false);
            dbCart.update("cartItems", FieldValue.arrayRemove(cartItem))
                    .addOnSuccessListener(unused -> {
                        cartItem.setQuantity(cartItem.getQuantity() + 1);
                        cartItemTotalChanged(cartItem, portionPrice.get(), holder, position);
                        dbCart.update("cartItems", FieldValue.arrayUnion(cartItem))
                                .addOnSuccessListener(unused1 -> {
                                    holder.cartItemQtyAddBtn.setClickable(true);
                                });
                    });
        });

        holder.cartItemQtyRemoveBtn.setOnClickListener(v -> {
            if (cartItem.getQuantity() > 1) {
                holder.cartItemQtyRemoveBtn.setClickable(false);
                dbCart.update("cartItems", FieldValue.arrayRemove(cartItem))
                        .addOnSuccessListener(unused -> {
                            cartItem.setQuantity(cartItem.getQuantity() - 1);
                            cartItemTotalChanged(cartItem, portionPrice.get(), holder, position);
                            dbCart.update("cartItems", FieldValue.arrayUnion(cartItem))
                                    .addOnSuccessListener(unused1 -> {
                                        holder.cartItemQtyRemoveBtn.setClickable(true);
                                    });
                        });
            }
        });

        holder.cartItemDeleteBtn.setOnClickListener(v -> {
            dbCart.update("cartItems", FieldValue.arrayRemove(cartItem))
                    .addOnSuccessListener(unused -> {
                        cartItems.remove(position);
                        notifyItemRemoved(position);
                        if (getItemCount() < 1) {
                            listener.onItemListEmpty();
                        }
                        cartItemTotals[position] = 0;
                        setTotal();
                    });
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    private void cartItemTotalChanged(Cart.CartItem cartItem, double portionPrice, ViewHolder holder, int position) {
        double cartItemTotal = cartItem.getQuantity() * portionPrice;
        cartItemTotals[position] = cartItemTotal;
        holder.cartItemQuantity.setText(String.valueOf(cartItem.getQuantity()));
        holder.cartItemPrice.setText(String.format(Locale.US, "LKR %,.2f",
                cartItemTotal));
        setTotal();
    }

    private void setTotal() {
        double total = 0;
        for (double cartItemTotal : cartItemTotals) {
            total += cartItemTotal;
        }
        listener.onTotalChange(total);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView cartItemImage;
        MaterialTextView cartItemName, cartItemPortion, cartItemPrice, cartItemQuantity;

        MaterialButton cartItemQtyAddBtn, cartItemQtyRemoveBtn, cartItemDeleteBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cartItemImage = itemView.findViewById(R.id.cart_item_image);
            cartItemName = itemView.findViewById(R.id.cart_item_name);
            cartItemPortion = itemView.findViewById(R.id.cart_item_portion);
            cartItemPrice = itemView.findViewById(R.id.cart_item_price);
            cartItemQuantity = itemView.findViewById(R.id.cart_item_quantity);
            cartItemQtyAddBtn = itemView.findViewById(R.id.cart_item_add_quantity);
            cartItemQtyRemoveBtn = itemView.findViewById(R.id.cart_item_remove_quantity);
            cartItemDeleteBtn = itemView.findViewById(R.id.cart_item_delete_btn);
        }
    }

    public interface CartItemListener {
        void onTotalChange(double total);

        void onItemListEmpty();
    }
}
