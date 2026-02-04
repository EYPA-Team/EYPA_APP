package com.eypa.app.ui.detail;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class DetailPagerAdapter extends FragmentStateAdapter {

    public DetailPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            return new DetailCommentsFragment();
        }
        return new DetailContentFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}