package com.lanzou.split.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.split.R;
import com.lanzou.split.data.LanzouFolder;
import com.lanzou.split.event.OnItemClickListener;
import com.lanzou.split.network.Repository;

import java.util.ArrayList;
import java.util.List;

public class FolderListAdapter extends ListAdapter<LanzouFolder, RecyclerView.ViewHolder> {

    private OnItemClickListener listener;

    private final List<LanzouFolder> sources = getCurrentList();

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