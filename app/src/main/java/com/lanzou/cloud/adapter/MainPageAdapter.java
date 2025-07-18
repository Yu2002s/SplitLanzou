package com.lanzou.cloud.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.lanzou.cloud.ui.file.FileFragment;
import com.lanzou.cloud.ui.fragment.MeFragment;
import com.lanzou.cloud.ui.transmission.TransmissionFragment;

public class MainPageAdapter extends FragmentStateAdapter {

    public MainPageAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new FileFragment();
            case 1: return new TransmissionFragment();
            default: return new MeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
