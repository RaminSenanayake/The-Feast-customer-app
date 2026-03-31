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

import lk.raminsenanayake.thefeast.R;
import lk.raminsenanayake.thefeast.databinding.CarouselItemBinding;
import lk.raminsenanayake.thefeast.model.CarouselItem;
import lk.raminsenanayake.thefeast.module.GlideApp;

public class CarousalAdapter extends RecyclerView.Adapter<CarousalAdapter.ViewHolder> {

    private List<CarouselItem> carouselItems;

    public CarousalAdapter(List<CarouselItem> carouselItems) {
        this.carouselItems = carouselItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CarouselItemBinding itemBinding = CarouselItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CarouselItem carouselItem = carouselItems.get(position);

        holder.title.setText(carouselItem.getName());
        holder.description.setText(carouselItem.getDescription());

        StorageReference carouselImage = FirebaseStorage.getInstance().getReference("carousel")
                .child(carouselItem.getImage());

        GlideApp.with(holder.itemView)
                .load(carouselImage)
                .into(holder.carouselItem);
    }

    @Override
    public int getItemCount() {
        return carouselItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView carouselItem;
        MaterialTextView title, description;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            carouselItem = itemView.findViewById(R.id.carousel_image_view);
            title = itemView.findViewById(R.id.carousel_item_title);
            description = itemView.findViewById(R.id.carousel_item_description);
        }
    }
}
