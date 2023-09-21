package com.lanzou.cloud.ui.transmission;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.lanzou.cloud.databinding.FragmentTransmissionBinding;
import com.lanzou.cloud.ui.download.DownloadListFragment;
import com.lanzou.cloud.ui.upload.UploadListFragment;

import java.util.ArrayList;
import java.util.List;

public class TransmissionFragment extends Fragment {

    private FragmentTransmissionBinding binding;

    private final List<Fragment> fragments = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTransmissionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fragments.add(new DownloadListFragment());
        fragments.add(new UploadListFragment());

        ViewPager2 viewPager2 = binding.viewPager2;
        viewPager2.setAdapter(new TransmissionAdapter(getChildFragmentManager(), getLifecycle()));

        new TabLayoutMediator(binding.tabLayout, viewPager2, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                if (position == 0) {
                    tab.setText("下载");
                } else {
                    tab.setText("上传");
                }

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
