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
import com.google.firebase.firestore.Query;

import java.util.List;

import lk.raminsenanayake.thefeast.adapter.OrdersAdapter;
import lk.raminsenanayake.thefeast.databinding.FragmentOrdersBinding;
import lk.raminsenanayake.thefeast.model.Order;

public class OrdersFragment extends Fragment {
    private final String TAG = OrdersFragment.class.getSimpleName();
    private FragmentOrdersBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("uid", FirebaseAuth.getInstance().getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "order retrieval failed: ", error);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        binding.ordersRecyclerView.setVisibility(View.VISIBLE);
                        binding.ordersEmptyView.setVisibility(View.GONE);
                        List<Order> orderList = value.toObjects(Order.class);
                        binding.ordersRecyclerView.setAdapter(new OrdersAdapter(orderList));
                        binding.ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    }else {
                        binding.ordersRecyclerView.setVisibility(View.GONE);
                        binding.ordersEmptyView.setVisibility(View.VISIBLE);
                    }
                });
    }
}