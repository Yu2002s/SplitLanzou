package com.lanzou.cloud.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.cloud.R;
import com.lanzou.cloud.data.LanzouFolder;
import com.lanzou.cloud.event.OnItemClickListener;

public class FolderListAdapter extends ListAdapter<LanzouFolder, FolderListAdapter.ViewHolder> {

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
    public FolderListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_folder, parent, false);
        ViewHolder viewHolder = new ViewHolder(itemView);
        itemView.setOnClickListener(v -> listener.onItemClick(viewHolder.getAbsoluteAdapterPosition(), v));
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull FolderListAdapter.ViewHolder holder, int position) {
        LanzouFolder lanzouFolder = getItem(position);
        holder.title.setText(lanzouFolder.getFolder_name());
        holder.sub.setText(String.valueOf(lanzouFolder.getFolder_id()));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView sub;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            sub = itemView.findViewById(R.id.tv_sub);
        }
    }
}