package lk.raminsenanayake.thefeast.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.raminsenanayake.thefeast.adapter.FavouritesAdapter;
import lk.raminsenanayake.thefeast.databinding.FragmentFavouritesBinding;
import lk.raminsenanayake.thefeast.model.Favourite;

public class FavouritesFragment extends Fragment {

    private final String TAG = "FavouritesFragment";
    private FragmentFavouritesBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFavouritesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.favouritesRecyclerView.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));
        loadData();
    }

    public void loadData() {
        FirebaseFirestore.getInstance().collection("users")
                .document(FirebaseAuth.getInstance().getUid()).collection("favourites")
                .get()
                .addOnSuccessListener(qds -> {
                    if (qds.isEmpty()) {
                        binding.favouritesEmptyView.setVisibility(View.VISIBLE);
                        binding.favouritesRecyclerView.setVisibility(View.GONE);
                        return;
                    }

                    binding.favouritesEmptyView.setVisibility(View.GONE);
                    binding.favouritesRecyclerView.setVisibility(View.VISIBLE);
                    binding.favouritesRecyclerView.setAdapter(new FavouritesAdapter(qds.toObjects(Favourite.class), () -> {
                        binding.favouritesEmptyView.setVisibility(View.VISIBLE);
                        binding.favouritesRecyclerView.setVisibility(View.GONE);
                    }));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "failed to load favourites: ", e);
                });
    }
}