package com.lanzou.cloud.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.lanzou.cloud.ui.cloud.file.FileFragment;
import com.lanzou.cloud.ui.fragment.HomeFragment;
import com.lanzou.cloud.ui.fragment.MeFragment;
import com.lanzou.cloud.ui.fragment.SettingFragment;
import com.lanzou.cloud.ui.fragment.TransmissionFragment;

public class MainPageAdapter extends FragmentStateAdapter {

    public MainPageAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new HomeFragment();
            case 1:
                return new FileFragment();
            case 2:
                return new TransmissionFragment();
            case 3:
                return new MeFragment();
            case 4:
                return new SettingFragment();
            default:
                throw new NullPointerException();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
