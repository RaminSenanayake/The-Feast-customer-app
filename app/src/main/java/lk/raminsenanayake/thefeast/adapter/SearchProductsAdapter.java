package lk.raminsenanayake.thefeast.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lk.raminsenanayake.thefeast.R;
import lk.raminsenanayake.thefeast.databinding.SearchItemListingBinding;
import lk.raminsenanayake.thefeast.listener.onItemClickListener;
import lk.raminsenanayake.thefeast.model.Product;
import lk.raminsenanayake.thefeast.module.GlideApp;

public class SearchProductsAdapter extends RecyclerView.Adapter<SearchProductsAdapter.ViewHolder> {

    private List<Product> productList, filteredProductList;
    private String searchString;
    private onItemClickListener itemClickListener;

    public SearchProductsAdapter(List<Product> productList, onItemClickListener itemClickListener) {
        this.productList = productList;
        this.searchString = "";
        this.itemClickListener = itemClickListener;
    }

    public void search(String searchText) {
        this.searchString = searchText.trim();
        this.filteredProductList = productList.stream().filter(new Predicate<Product>() {
            @Override
            public boolean test(Product product) {
                return product.getName().toLowerCase().contains(searchString.toLowerCase());
            }
        }).collect(Collectors.toList());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SearchItemListingBinding binding = SearchItemListingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = searchString.isEmpty() ? productList.get(position) : filteredProductList.get(position);
        holder.productName.setText(product.getName());

        double start, end;
        Map<String, Double> portionList = product.getPortionList();
        List<Double> sortedList = portionList.values().stream().sorted().collect(Collectors.toList());
        start = sortedList.get(0);
        end = portionList.size() > 1 ? sortedList.get(portionList.size() - 1) : 0;

        if (end == 0) {
            holder.productPrice.setText(String.format(Locale.US, "LKR %,.2f", start));
        } else {
            holder.productPrice.setText(String.format(Locale.US, "LKR %,.2f - %,.2f", start, end));
        }

        if (product.getImage() != null && !product.getImage().isEmpty()) {
            StorageReference image = FirebaseStorage.getInstance().getReference("product-images")
                    .child(product.getImage());

            GlideApp.with(holder.itemView)
                    .load(image)
                    .into(holder.productImage);
        } else {
            GlideApp.with(holder.itemView)
                    .load(R.drawable.logo_transparent)
                    .into(holder.productImage);
        }

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return searchString.isEmpty() ? productList.size() : filteredProductList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView productImage;
        private MaterialTextView productName, productPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
        }
    }
}
