package com.lanzou.cloud.ui.selector;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;
import com.lanzou.cloud.R;
import com.lanzou.cloud.base.BaseActivity;
import com.lanzou.cloud.data.FileInfo;
import com.lanzou.cloud.databinding.ActivityFileSelectorBinding;

import java.util.ArrayList;
import java.util.List;

public class FileSelectorActivity extends AppCompatActivity {

    private ActivityFileSelectorBinding binding;

    private final List<FileInfo> selectedFiles = new ArrayList<>();

    public List<FileInfo> getSelectedFiles() {
        return selectedFiles;
    }

    private String searchWorld = "";

    public String getSearchWorld() {
        return searchWorld;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityFileSelectorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.header.toolBar.setContentInsetStartWithNavigation(0);
        setSupportActionBar(binding.header.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        SearchView searchView = new SearchView(this);
        searchView.setQueryHint("输入关键字进行搜索");
        searchView.post(searchView::clearFocus);
        searchView.onActionViewExpanded();
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(searchView);

        String[] titles = {"最近", "软件", "安装包", "图片", "音频", "视频", "文档", /*"QQ", "微信"*/};
        List<FileSelectorFragment> fragmentList = new ArrayList<>();
        for (int i = 0; i < titles.length; i++) {
            fragmentList.add(FileSelectorFragment.newInstance(i));
        }

        ViewPager2 viewpager2 = binding.viewpager2;
        viewpager2.setOffscreenPageLimit(titles.length + 1);
        viewpager2.setAdapter(new FragmentStateAdapter(getSupportFragmentManager(), getLifecycle()) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return fragmentList.get(position);
            }

            @Override
            public int getItemCount() {
                return titles.length;
            }
        });
        new TabLayoutMediator(binding.tabLayout, viewpager2, (tab, position) ->
                tab.setText(titles[position])
        ).attach();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // 直接进行搜索
                searchWorld = newText;
                for (FileSelectorFragment fragment : fragmentList) {
                    fragment.onSearch(newText);
                }
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_file_selector, menu);
        MenuItem item = menu.findItem(R.id.upload_file);
        Button upload = item.getActionView().findViewById(R.id.btn_upload);
        upload.setOnClickListener(v -> {
            ArrayList<CharSequence> uris = new ArrayList<>();
            for (int i = 0; i < selectedFiles.size(); i++) {
                uris.add(selectedFiles.get(i).getUri());
            }
            setResult(RESULT_OK, new Intent().putCharSequenceArrayListExtra("files", uris));
            finish();
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
