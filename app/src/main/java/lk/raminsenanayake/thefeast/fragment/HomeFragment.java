package lk.raminsenanayake.thefeast.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.carousel.CarouselLayoutManager;
import com.google.android.material.carousel.CarouselSnapHelper;
import com.google.android.material.carousel.FullScreenCarouselStrategy;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import lk.raminsenanayake.thefeast.R;
import lk.raminsenanayake.thefeast.activity.SingleProductActivity;
import lk.raminsenanayake.thefeast.adapter.CarousalAdapter;
import lk.raminsenanayake.thefeast.adapter.HomeProductsAdapter;
import lk.raminsenanayake.thefeast.adapter.SearchProductsAdapter;
import lk.raminsenanayake.thefeast.databinding.FragmentHomeBinding;
import lk.raminsenanayake.thefeast.listener.onItemClickListener;
import lk.raminsenanayake.thefeast.model.CarouselItem;
import lk.raminsenanayake.thefeast.model.Category;
import lk.raminsenanayake.thefeast.model.Product;
import lk.raminsenanayake.thefeast.module.GridSpaceDecoration;
import lk.raminsenanayake.thefeast.module.HomeCarouselLayoutManager;

public class HomeFragment extends Fragment {

    private final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private List<CarouselItem> carouselItems;
    private CarouselLayoutManager carouselLayoutManager;
    private SearchProductsAdapter searchProductsAdapter;
    private HomeProductsAdapter homeProductsAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        carouselItems = new ArrayList<>();
        FirebaseFirestore.getInstance().collection("carouselItems")
                .get()
                .addOnSuccessListener(task -> {
                    if (!task.isEmpty()) {
                        carouselItems.addAll(task.toObjects(CarouselItem.class));

                        binding.carouselShimmerLayout.stopShimmer();
                        binding.carouselShimmerLayout.setVisibility(View.GONE);
                        binding.carouselRecyclerView.setVisibility(View.VISIBLE);

                        autoScroll();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Images loading failed", e);
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(getActivity(), getActivity().findViewById(R.id.main), binding.toolbar, R.string.drawer_open, R.string.drawer_close);
        DrawerLayout drawerLayout = getActivity().findViewById(R.id.main);
        drawerLayout.addDrawerListener(toggle);

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.contact_us) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:0713792339"));
                startActivity(intent);
                return true;
            }
            return false;
        });

        carouselLayoutManager = new HomeCarouselLayoutManager();
        carouselLayoutManager.setCarouselStrategy(new FullScreenCarouselStrategy());
        binding.carouselRecyclerView.setLayoutManager(carouselLayoutManager);
        new CarouselSnapHelper().attachToRecyclerView(binding.carouselRecyclerView);

        binding.carouselShimmerLayout.startShimmer();
        binding.homeChipGroupShimmerLayout.startShimmer();
        binding.homeProductsShimmerLayout.startShimmer();

        CarousalAdapter carousalAdapter = new CarousalAdapter(carouselItems);
        binding.carouselRecyclerView.setAdapter(carousalAdapter);

        FirebaseFirestore.getInstance().collection("categories")
                .get()
                .addOnSuccessListener(task -> {
                    if (!task.isEmpty()) {
                        List<Category> categories = task.toObjects(Category.class);
                        for (Category category : categories) {
                            Chip chip = (Chip) LayoutInflater.from(view.getContext())
                                    .inflate(R.layout.radio_chip_layout, binding.homeChipGroup, false);
                            chip.setTag(category.getId());
                            chip.setText(category.getName());
                            binding.homeChipGroup.addView(chip);

                            chip.setOnCheckedChangeListener((chip1, isChecked) -> {
                                if (isChecked) {
                                    homeProductsAdapter.setFilter(String.valueOf(chip1.getTag()));
                                }
                            });
                        }

                        binding.homeChipGroupShimmerLayout.stopShimmer();
                        binding.homeChipGroupShimmerLayout.setVisibility(View.GONE);
                        binding.homeChipGroupHorizontalView.setVisibility(View.VISIBLE);
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "category load failed: ", e);
                });

        binding.homeAllChip.setOnCheckedChangeListener((chip, isChecked) -> {
            if (isChecked) {
                homeProductsAdapter.setFilter("");
            }
        });

        FirebaseFirestore.getInstance().collection("products")
                .get()
                .addOnSuccessListener(task -> {
                    if (!task.isEmpty()) {
                        List<Product> products = task.toObjects(Product.class);

                        binding.homeProductsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
                        binding.homeProductsRecyclerView.addItemDecoration(new GridSpaceDecoration(20));

                        onItemClickListener listener = product -> {
                            Intent intent = new Intent(getContext(), SingleProductActivity.class);
                            intent.putExtra("product", product);
                            startActivity(intent);
                        };

                        homeProductsAdapter = new HomeProductsAdapter(products, listener);
                        binding.homeProductsRecyclerView.setAdapter(homeProductsAdapter);
                        binding.homeProductsShimmerLayout.stopShimmer();
                        binding.homeProductsShimmerLayout.setVisibility(View.GONE);
                        binding.homeProductsRecyclerView.setVisibility(View.VISIBLE);

                        searchProductsAdapter = new SearchProductsAdapter(products, listener);
                        binding.homeSearchProductsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                        binding.homeSearchProductsRecyclerView.setAdapter(searchProductsAdapter);
                        Log.d(TAG, "products loaded");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "home products loading failed: ", e);
                });

        binding.homeSearchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (searchProductsAdapter != null) {
                    searchProductsAdapter.search(s.toString());
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });
    }

    private int i = 0;
    private final long waitingMillis = 5000;

    public void autoScroll() {
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                i = i < binding.carouselRecyclerView.getAdapter().getItemCount() - 1 ? i + 1 : 0;
                binding.carouselRecyclerView.smoothScrollToPosition(i);
                handler.postDelayed(this, waitingMillis);
            }
        };
        handler.postDelayed(runnable, waitingMillis);
    }
}