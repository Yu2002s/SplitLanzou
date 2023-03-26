package com.lanzou.split.ui.folder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.lanzou.split.R;
import com.lanzou.split.base.BaseActivity;
import com.lanzou.split.data.LanzouFolder;
import com.lanzou.split.databinding.ActivityFolderSelectorBinding;
import com.lanzou.split.event.OnItemClickListener;
import com.lanzou.split.network.Repository;

import java.util.List;

public class FolderSelectorActivity extends BaseActivity {

    private ActivityFolderSelectorBinding binding;

    private List<LanzouFolder> lanzouFolders;

    private FolderListAdapter folderListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFolderSelectorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.header.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = binding.folderRecycler;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
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
                intent.putExtra("id", lanzouFolders.get(position).getFolder_id());
                setResult(RESULT_OK, intent);
                finish();
            }
        });

    }

    private void getFolders() {
        new Thread(() -> {
            lanzouFolders = Repository.getInstance().getAllFolder();
            runOnUiThread(() -> {
                folderListAdapter.submitList(lanzouFolders);
                binding.refresh.setRefreshing(false);
            });
        }).start();
    }

    private static class FolderListAdapter extends ListAdapter<LanzouFolder, RecyclerView.ViewHolder> {

        private OnItemClickListener listener;

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        public FolderListAdapter() {
            super(new DiffUtil.ItemCallback<LanzouFolder>() {
                @Override
                public boolean areItemsTheSame(@NonNull LanzouFolder oldItem, @NonNull LanzouFolder newItem) {
                    return oldItem == newItem;
                }

                @Override
                public boolean areContentsTheSame(@NonNull LanzouFolder oldItem, @NonNull LanzouFolder newItem) {
                    return oldItem.getFolder_id() == newItem.getFolder_id();
                }
            });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_list_folder, parent, false);
            RecyclerView.ViewHolder viewHolder = new RecyclerView.ViewHolder(itemView) {
            };
            itemView.setOnClickListener(v -> listener.onItemClick(viewHolder.getAdapterPosition(), v));
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ((TextView) holder.itemView).setText(getItem(position).getFolder_name());
        }
    }
}
