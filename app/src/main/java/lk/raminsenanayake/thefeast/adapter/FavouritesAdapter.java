package lk.raminsenanayake.thefeast.adapter;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

import lk.raminsenanayake.thefeast.R;
import lk.raminsenanayake.thefeast.activity.FavouriteItemsActivity;
import lk.raminsenanayake.thefeast.databinding.FavouritesListingBinding;
import lk.raminsenanayake.thefeast.model.Favourite;

public class FavouritesAdapter extends RecyclerView.Adapter<FavouritesAdapter.ViewHolder> {
    private final String TAG = "FavouritesAdapter";
    private List<Favourite> favouriteList;
    private FavouritesListingBinding binding;
    private OnListEmptyListener listener;

    public FavouritesAdapter(List<Favourite> favouriteList, OnListEmptyListener listener) {
        this.favouriteList = favouriteList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        binding = FavouritesListingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Favourite favourite = favouriteList.get(position);
        holder.favouritesName.setText(favourite.getName());
        holder.favouritesTotal.setText(String.format(Locale.US, "Total: LKR %.2f", favourite.getTotal()));
        holder.favouritesDeleteBtn.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(binding.getRoot().getContext())
                    .setTitle("Delete Favourite")
                    .setMessage("Are you sure you want to delete " + favourite.getName() + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseFirestore.getInstance().collection("users")
                                .document(FirebaseAuth.getInstance().getUid()).collection("favourites")
                                .whereEqualTo("name", favourite.getName())
                                .get()
                                .addOnSuccessListener(qds -> {
                                    qds.getDocuments().get(0).getReference().delete()
                                            .addOnSuccessListener(unused -> {
                                                favouriteList.remove(favourite);
                                                notifyItemRemoved(position);
                                                Snackbar.make(binding.getRoot(),
                                                                "Favourite deleted", Snackbar.LENGTH_SHORT)
                                                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                                        .show();
                                                if (favouriteList.isEmpty()) {
                                                    listener.onListEmpty();
                                                }
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "failed to delete favourite: ", e);
                                });
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    }).show();

        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), FavouriteItemsActivity.class);
            intent.putExtra("favourite", favourite);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return favouriteList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        MaterialTextView favouritesName, favouritesTotal;
        MaterialButton favouritesDeleteBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            favouritesName = itemView.findViewById(R.id.favourites_name);
            favouritesTotal = itemView.findViewById(R.id.favourites_total);
            favouritesDeleteBtn = itemView.findViewById(R.id.favourites_delete_btn);
        }
    }

    public interface OnListEmptyListener{
        void onListEmpty();
    }
}
