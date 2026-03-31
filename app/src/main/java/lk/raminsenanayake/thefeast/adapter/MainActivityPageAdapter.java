package lk.raminsenanayake.thefeast.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import lk.raminsenanayake.thefeast.fragment.CartFragment;
import lk.raminsenanayake.thefeast.fragment.HomeFragment;
import lk.raminsenanayake.thefeast.fragment.OrdersFragment;
import lk.raminsenanayake.thefeast.fragment.ProfileFragment;
import lk.raminsenanayake.thefeast.fragment.FavouritesFragment;
import lombok.Getter;

public class MainActivityPageAdapter extends FragmentStateAdapter {

    @Getter
    private CartFragment cartFragment;
    @Getter
    private FavouritesFragment favouritesFragment;
    public MainActivityPageAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = null;
        switch (position) {
            case 0:
                fragment = new HomeFragment();
                break;
            case 1:
                fragment = new OrdersFragment();
                break;
            case 2:
                fragment = new FavouritesFragment();
                favouritesFragment = (FavouritesFragment) fragment;
                break;
            case 3:
                fragment = new CartFragment();
                cartFragment = (CartFragment) fragment;
                break;
            case 4:
                fragment = new ProfileFragment();
                break;
        }
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
