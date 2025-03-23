package com.lanzou.cloud.ui.upload;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.lanzou.cloud.R;
import com.lanzou.cloud.adapter.FolderListAdapter;
import com.lanzou.cloud.data.LanzouFolder;
import com.lanzou.cloud.data.LanzouPage;
import com.lanzou.cloud.databinding.ActivityExternalUploadBinding;
import com.lanzou.cloud.network.Repository;
import com.lanzou.cloud.service.UploadService;

import java.util.ArrayList;
import java.util.List;

public class ExternalUploadActivity extends AppCompatActivity implements ServiceConnection {

    private UploadService uploadService;

    private ActivityExternalUploadBinding binding;

    private List<LanzouFolder> folders;

    private FolderListAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, UploadService.class));
        bindService(new Intent(this,  UploadService.class), this, BIND_AUTO_CREATE);
        binding = ActivityExternalUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.header.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.fileRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        adapter = new FolderListAdapter();
        binding.fileRecyclerView.setAdapter(adapter);
        new Thread(() -> {
            folders = Repository.getInstance().getAllFolder();
            if (folders == null) {
                finish();
                return;
            }
            runOnUiThread(() -> adapter.submitList(folders));
        }).start();

        adapter.setOnItemClickListener((position, view) -> {
            LanzouPage lanzouPage = new LanzouPage();
            LanzouFolder lanzouFolder = adapter.getCurrentList().get(position);
            lanzouPage.setName(lanzouFolder.getFolder_name());
            lanzouPage.setFolderId(lanzouFolder.getFolder_id());

            Intent intent = getIntent();
            if (intent.getData() != null) {
                uploadService.uploadFile(intent.getData().toString(), lanzouPage);
                return;
            }

            ClipData clipData = intent.getClipData();
            if (clipData == null) {
                return;
            }
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                uploadService.uploadFile(item.getUri().toString(), lanzouPage);
            }

            finish();
        });

    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        uploadService = ((UploadService.UploadBinder) service).getService();

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

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
                for (LanzouFolder lanzouFolder : folders) {
                    if (lanzouFolder.getFolder_name().toLowerCase().contains(newText.toLowerCase())) {
                        filteredList.add(lanzouFolder);
                    }
                }
                adapter.submitList(filteredList);
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }
}
