package lk.raminsenanayake.thefeast.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;
import java.util.Locale;

import lk.raminsenanayake.thefeast.R;
import lk.raminsenanayake.thefeast.activity.OrderItemsActivity;
import lk.raminsenanayake.thefeast.databinding.OrdersListingBinding;
import lk.raminsenanayake.thefeast.model.Order;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.ViewHolder> {

    private List<Order> orderList;
    private OrdersListingBinding binding;

    public OrdersAdapter(List<Order> orderList) {
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        binding = OrdersListingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.orderId.setText(order.getOrderId());
        holder.orderTotal.setText(String.format(Locale.US, "Total: LKR %,.2f", order.getTotal()));
        holder.orderStatus.setText(order.getStatus());
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(binding.getRoot().getContext(), OrderItemsActivity.class);
            intent.putExtra("order", order);
            binding.getRoot().getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        MaterialTextView orderId, orderTotal;
        Chip orderStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.order_id);
            orderTotal = itemView.findViewById(R.id.order_total);
            orderStatus = itemView.findViewById(R.id.order_status);
        }
    }
}
