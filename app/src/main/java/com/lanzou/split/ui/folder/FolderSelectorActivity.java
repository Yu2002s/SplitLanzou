package com.lanzou.split.ui.folder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.lanzou.split.R;
import com.lanzou.split.adapter.FolderListAdapter;
import com.lanzou.split.base.BaseActivity;
import com.lanzou.split.data.LanzouFile;
import com.lanzou.split.data.LanzouFolder;
import com.lanzou.split.databinding.ActivityFolderSelectorBinding;
import com.lanzou.split.event.OnItemClickListener;
import com.lanzou.split.network.Repository;

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

    public static void moveFiles(Context context, ActivityResultLauncher<Intent> launcher,
                                 ArrayList<LanzouFile> lanzouFiles) {
        launcher.launch(new Intent(context, FolderSelectorActivity.class)
                .putParcelableArrayListExtra("lanzouFiles", lanzouFiles));
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

        binding.refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getFolders();
            }
        });

        folderListAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position, View view) {
                Intent intent = new Intent();
                intent.putExtra("id", folderListAdapter.getCurrentList().get(position).getFolder_id());
                LanzouFile lanzouFile = getIntent().getParcelableExtra("lanzouFile");
                intent.putExtra("lanzouFile", lanzouFile);
                ArrayList<Parcelable> lanzouFiles = getIntent().getParcelableArrayListExtra("lanzouFiles");
                intent.putExtra("lanzouFiles", lanzouFiles);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        binding.add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(FolderSelectorActivity.this, "前往主页新建文件夹", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void getFolders() {
        new Thread(() -> {
            lanzouFolders = Repository.getInstance().getAllFolder();
            if (lanzouFolders == null) {
                lanzouFolders = new ArrayList<>();
            }
            lanzouFolders.add(0, new LanzouFolder(-1, "根目录"));
            for (LanzouFolder lanzouFolder : lanzouFolders) {
                lanzouFolder.setFolder_name(lanzouFolder.getFolder_name() + "(" + lanzouFolder.getFolder_id() + ")");
            }
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
