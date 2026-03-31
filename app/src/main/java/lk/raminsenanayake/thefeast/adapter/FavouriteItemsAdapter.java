package lk.raminsenanayake.thefeast.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import lk.raminsenanayake.thefeast.R;
import lk.raminsenanayake.thefeast.databinding.FavouriteItemCardBinding;
import lk.raminsenanayake.thefeast.model.Cart;
import lk.raminsenanayake.thefeast.model.Product;
import lk.raminsenanayake.thefeast.module.GlideApp;

public class FavouriteItemsAdapter extends RecyclerView.Adapter<FavouriteItemsAdapter.ViewHolder> {

    private final String TAG = "FavouriteItemsAdapter";
    private List<Cart.CartItem> favouriteListCartItems;
    private FavouriteItemCardBinding binding;
    private double[] favouriteItemTotals;
    private FavouriteItemListener listener;
    private DocumentReference favouriteOrder;

    public FavouriteItemsAdapter(List<Cart.CartItem> favouriteListCartItems, String favouriteName, FavouriteItemListener listener) {
        this.favouriteListCartItems = favouriteListCartItems;
        this.listener = listener;
        favouriteItemTotals = new double[favouriteListCartItems.size()];
        FirebaseFirestore.getInstance().collection("users")
                .document(FirebaseAuth.getInstance().getUid()).collection("favourites")
                .whereEqualTo("name", favouriteName)
                .get()
                .addOnSuccessListener(qds -> {
                    favouriteOrder = qds.getDocuments().get(0).getReference();
                });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        binding = FavouriteItemCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cart.CartItem favouriteListCartItem = favouriteListCartItems.get(position);
        AtomicReference<Double> portionPrice = new AtomicReference<>((double) 0);

        FirebaseFirestore.getInstance().collection("products")
                .whereEqualTo("productId", favouriteListCartItem.getProductId())
                .get()
                .addOnSuccessListener(qds -> {
                    Product product = qds.toObjects(Product.class).get(0);

                    holder.favouriteItemName.setText(product.getName());

                    for (Map.Entry<String, Double> portion : product.getPortionList().entrySet()) {
                        if (portion.getKey().equals(favouriteListCartItem.getPortionSize())) {
                            portionPrice.set(portion.getValue());
                            favouriteItemTotalChanged(favouriteListCartItem, portionPrice.get(), holder, position);
                            break;
                        }
                    }

                    if (!product.getImage().isEmpty()) {
                        StorageReference image = FirebaseStorage.getInstance().getReference("product-images")
                                .child(product.getImage());

                        GlideApp.with(holder.itemView)
                                .load(image)
                                .into(holder.favouriteItemImage);
                    } else {
                        GlideApp.with(holder.itemView)
                                .load(R.drawable.logo_transparent)
                                .into(holder.favouriteItemImage);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to obtain product id: ", e);
                });

        holder.favouriteItemPortion.setText(favouriteListCartItem.getPortionSize());

        holder.favouriteItemQtyAddBtn.setOnClickListener(v -> {
            favouriteListCartItem.setQuantity(favouriteListCartItem.getQuantity() + 1);
            favouriteItemTotalChanged(favouriteListCartItem, portionPrice.get(), holder, position);
        });

        holder.favouriteItemQtyRemoveBtn.setOnClickListener(v -> {
            if (favouriteListCartItem.getQuantity() > 1) {
                favouriteListCartItem.setQuantity(favouriteListCartItem.getQuantity() - 1);
                favouriteItemTotalChanged(favouriteListCartItem, portionPrice.get(), holder, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return favouriteListCartItems.size();
    }

    private void favouriteItemTotalChanged(Cart.CartItem cartItem, double portionPrice, ViewHolder holder, int position) {
        double cartItemTotal = cartItem.getQuantity() * portionPrice;
        favouriteItemTotals[position] = cartItemTotal;
        holder.favouriteItemQuantity.setText(String.valueOf(cartItem.getQuantity()));
        holder.favouriteItemPrice.setText(String.format(Locale.US, "LKR %,.2f",
                cartItemTotal));
        setTotal();
    }

    private void setTotal() {
        double total = 0;
        for (double favouriteItemTotal : favouriteItemTotals) {
            total += favouriteItemTotal;
        }
        listener.onTotalChange(total);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView favouriteItemImage;
        MaterialTextView favouriteItemName, favouriteItemPortion, favouriteItemPrice, favouriteItemQuantity;

        MaterialButton favouriteItemQtyAddBtn, favouriteItemQtyRemoveBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            favouriteItemImage = itemView.findViewById(R.id.favourite_item_image);
            favouriteItemName = itemView.findViewById(R.id.favourite_item_name);
            favouriteItemPortion = itemView.findViewById(R.id.favourite_item_portion);
            favouriteItemPrice = itemView.findViewById(R.id.favourite_item_price);
            favouriteItemQuantity = itemView.findViewById(R.id.favourite_item_quantity);
            favouriteItemQtyAddBtn = itemView.findViewById(R.id.favourite_item_add_quantity);
            favouriteItemQtyRemoveBtn = itemView.findViewById(R.id.favourite_item_remove_quantity);
        }
    }

    public interface FavouriteItemListener {
        void onTotalChange(double total);
    }
}
