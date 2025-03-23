package com.lanzou.cloud.ui.folder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.lanzou.cloud.R;
import com.lanzou.cloud.adapter.FolderListAdapter;
import com.lanzou.cloud.base.BaseActivity;
import com.lanzou.cloud.data.LanzouFile;
import com.lanzou.cloud.data.LanzouFolder;
import com.lanzou.cloud.databinding.ActivityFolderSelectorBinding;
import com.lanzou.cloud.network.Repository;

import java.util.ArrayList;
import java.util.List;

public class FolderSelectorActivity extends BaseActivity {

    private ActivityFolderSelectorBinding binding;

    private List<LanzouFolder> lanzouFolders;

    private FolderListAdapter folderListAdapter;

    public static void moveFile(Context context, ActivityResultLauncher<Intent> launcher,
                                LanzouFile lanzouFile) {
        launcher.launch(new Intent(context, FolderSelectorActivity.class)
                .putExtra("lanzouFile", lanzouFile)
        );
    }

    public static void moveFiles(Context context, ActivityResultLauncher<Intent> launcher) {
        launcher.launch(new Intent(context, FolderSelectorActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFolderSelectorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.header.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = binding.folderRecycler;
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        folderListAdapter = new FolderListAdapter();
        recyclerView.setAdapter(folderListAdapter);

        binding.refresh.setRefreshing(true);
        getFolders();

        binding.refresh.setOnRefreshListener(this::getFolders);

        folderListAdapter.setOnItemClickListener((position, view) -> {
            Intent intent = new Intent();
            intent.putExtra("id", folderListAdapter.getCurrentList().get(position).getFolder_id());
            LanzouFile lanzouFile = getIntent().getParcelableExtra("lanzouFile");
            intent.putExtra("lanzouFile", lanzouFile);
            setResult(RESULT_OK, intent);
            finish();
        });

        binding.add.setOnClickListener(v -> Toast.makeText(FolderSelectorActivity.this, "前往主页新建文件夹", Toast.LENGTH_SHORT).show());

    }

    private void getFolders() {
        new Thread(() -> {
            lanzouFolders = Repository.getInstance().getAllFolder();
            if (lanzouFolders == null) {
                lanzouFolders = new ArrayList<>();
            }
            lanzouFolders.add(0, new LanzouFolder(-1, "根目录"));
            runOnUiThread(() -> {
                folderListAdapter.submitList(lanzouFolders);
                binding.refresh.setRefreshing(false);
            });
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem search = menu.add("搜索");
        search.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW | MenuItem.SHOW_AS_ACTION_ALWAYS);
        search.setIcon(R.drawable.baseline_search_24);
        SearchView searchView = new SearchView(this);
        search.setActionView(searchView);
        searchView.setQueryHint("输入关键字搜索");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                List<LanzouFolder> filteredList = new ArrayList<>();
                for (LanzouFolder lanzouFolder : lanzouFolders) {
                    if (lanzouFolder.getFolder_name().toLowerCase().contains(newText.toLowerCase())) {
                        filteredList.add(lanzouFolder);
                    }
                }
                folderListAdapter.submitList(filteredList);
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }
}
