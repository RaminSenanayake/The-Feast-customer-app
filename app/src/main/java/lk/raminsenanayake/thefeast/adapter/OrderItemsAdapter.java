package lk.raminsenanayake.thefeast.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Locale;

import lk.raminsenanayake.thefeast.R;
import lk.raminsenanayake.thefeast.databinding.OrderItemCardBinding;
import lk.raminsenanayake.thefeast.model.Order;
import lk.raminsenanayake.thefeast.model.Product;
import lk.raminsenanayake.thefeast.module.GlideApp;

public class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.ViewHolder> {

    private final String TAG = OrderItemsAdapter.class.getSimpleName();
    private List<Order.OrderItem> orderItemList;

    public OrderItemsAdapter(List<Order.OrderItem> orderItemList) {
        this.orderItemList = orderItemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        OrderItemCardBinding binding = OrderItemCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order.OrderItem orderItem = orderItemList.get(position);
        FirebaseFirestore.getInstance().collection("products")
                .whereEqualTo("productId", orderItem.getProductId())
                .get()
                .addOnSuccessListener(qds -> {
                    Product product = qds.toObjects(Product.class).get(0);

                    holder.orderItemName.setText(product.getName());
                    holder.orderItemPortion.setText(orderItem.getPortionSize());
                    holder.orderItemPrice.setText(String.format(Locale.US, "LKR %,.2f",
                            product.getPortionList().get(orderItem.getPortionSize())));
                    holder.orderItemQty.setText(String.format(Locale.US, "Quantity: %d", orderItem.getQuantity()));

                    if (!product.getImage().isEmpty()) {
                        StorageReference image = FirebaseStorage.getInstance().getReference("product-images")
                                .child(product.getImage());

                        GlideApp.with(holder.itemView)
                                .load(image)
                                .into(holder.orderItemImage);
                    } else {
                        GlideApp.with(holder.itemView)
                                .load(R.drawable.logo_transparent)
                                .into(holder.orderItemImage);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "failed to get product: ", e);
                });
    }

    @Override
    public int getItemCount() {
        return orderItemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        MaterialTextView orderItemName, orderItemPortion, orderItemPrice, orderItemQty;
        ImageView orderItemImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderItemName = itemView.findViewById(R.id.order_item_name);
            orderItemPortion = itemView.findViewById(R.id.order_item_portion);
            orderItemPrice = itemView.findViewById(R.id.order_item_price);
            orderItemQty = itemView.findViewById(R.id.order_item_quantity);
            orderItemImage = itemView.findViewById(R.id.order_item_image);
        }
    }
}
