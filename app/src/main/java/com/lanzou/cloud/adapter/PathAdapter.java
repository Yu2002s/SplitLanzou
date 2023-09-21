package com.lanzou.cloud.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.cloud.data.LanzouPage;
import com.lanzou.cloud.databinding.ItemPathBinding;
import com.lanzou.cloud.event.OnItemClickListener;

import java.util.List;

public class PathAdapter extends RecyclerView.Adapter<PathAdapter.ViewHolder> {

    private final List<LanzouPage> list;

    public PathAdapter(List<LanzouPage> list) {
        this.list = list;
    }

    private OnItemClickListener listener;

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = ItemPathBinding.inflate(LayoutInflater.
                from(parent.getContext()), parent, false).getRoot();
        ViewHolder viewHolder = new ViewHolder(itemView);
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(viewHolder.getAdapterPosition(), v);
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ((TextView) holder.itemView).setText(list.get(position).getName());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

}
