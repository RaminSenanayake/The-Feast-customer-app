package lk.raminsenanayake.thefeast.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import lk.raminsenanayake.thefeast.fragment.SignInFragment;
import lk.raminsenanayake.thefeast.fragment.SignUpFragment;

public class AuthActivityPageAdapter extends FragmentStateAdapter {
    public AuthActivityPageAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = null;
        switch (position) {
            case 0:
                fragment = new SignInFragment();
                break;
            case 1:
                fragment = new SignUpFragment();
                break;
        }
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
