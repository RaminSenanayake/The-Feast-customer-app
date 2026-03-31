package lk.raminsenanayake.thefeast.activity;

import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import lk.raminsenanayake.thefeast.adapter.OrderItemsAdapter;
import lk.raminsenanayake.thefeast.databinding.ActivityOrderItemsBinding;
import lk.raminsenanayake.thefeast.model.Order;

public class OrderItemsActivity extends AppCompatActivity {
    private ActivityOrderItemsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityOrderItemsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Order order;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            order = getIntent().getSerializableExtra("order", Order.class);
        } else {
            order = (Order) getIntent().getSerializableExtra("order");
        }

        binding.toolbar.setTitle(order.getOrderId());
        binding.orderListRecyclerView.setAdapter(new OrderItemsAdapter(order.getOrderItems()));
        binding.orderListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}