package com.lanzou.cloud.ui.transmission;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;
import com.lanzou.cloud.base.BaseActivity;
import com.lanzou.cloud.databinding.ActivityTransmissionListBinding;
import com.lanzou.cloud.ui.download.DownloadListFragment;
import com.lanzou.cloud.ui.upload.UploadListFragment;

import java.util.ArrayList;
import java.util.List;

public class TransmissionListActivity extends BaseActivity {

    private ActivityTransmissionListBinding binding;

    private final List<Fragment> fragments = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransmissionListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.header.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fragments.add(new DownloadListFragment());
        fragments.add(new UploadListFragment());

        ViewPager2 viewPager2 = binding.viewPager2;
        viewPager2.setAdapter(new TransmissionAdapter(getSupportFragmentManager(), getLifecycle()));

        new TabLayoutMediator(binding.tabLayout, viewPager2, (tab, position) -> {
            if (position == 0) {
                tab.setText("下载");
            } else {
                tab.setText("上传");
            }

        }).attach();
    }

    private class TransmissionAdapter extends FragmentStateAdapter {

        public TransmissionAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragments.get(position);
        }


        @Override
        public int getItemCount() {
            return fragments.size();
        }
    }
}
