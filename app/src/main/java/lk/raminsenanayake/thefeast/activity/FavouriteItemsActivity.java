package lk.raminsenanayake.thefeast.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Locale;

import lk.raminsenanayake.thefeast.adapter.FavouriteItemsAdapter;
import lk.raminsenanayake.thefeast.databinding.ActivityFavouriteItemsBinding;
import lk.raminsenanayake.thefeast.model.Cart;
import lk.raminsenanayake.thefeast.model.Favourite;

public class FavouriteItemsActivity extends AppCompatActivity {

    private Cart cart;
    private double cartTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        ActivityFavouriteItemsBinding binding = ActivityFavouriteItemsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Favourite favourite = (Favourite) getIntent().getSerializableExtra("favourite");
        cart = favourite;

        binding.toolbar.setTitle(favourite.getName());
        binding.favouriteItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        binding.favouriteItemsRecyclerView.setAdapter(new FavouriteItemsAdapter(favourite.getCartItems(), favourite.getName(), total -> {
            cartTotal = total;
            binding.favouritesTotal.setText(String.format(Locale.US, "LKR %,.2f", total));
        }));

        binding.proceedBtn.setOnClickListener(this::goToCheckout);
    }

    private void goToCheckout(View v) {
        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra("total", cartTotal);
        intent.putExtra("cart", cart);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}