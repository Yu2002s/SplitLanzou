package com.lanzou.cloud.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lanzou.cloud.data.FileInfo;
import com.lanzou.cloud.databinding.ItemListFileSelectorBinding;
import com.lanzou.cloud.event.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

public class FileSelectorAdapter extends RecyclerView.Adapter<FileSelectorAdapter.ViewHolder> implements Filterable {


    private List<FileInfo> sources;

    private List<FileInfo> files;

    public FileSelectorAdapter(List<FileInfo> files) {
        this.files = files;
        sources = files;
    }

    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void notifySelect(int position) {
        notifyItemChanged(position, 1);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemListFileSelectorBinding binding = ItemListFileSelectorBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);
        ViewHolder viewHolder = new ViewHolder(binding);
        viewHolder.itemView.setOnClickListener(v -> {
            v.setSelected(!v.isSelected());
            int position = viewHolder.getAbsoluteAdapterPosition();
            files.get(position).setSelected(v.isSelected());
            onItemClickListener.onItemClick(position, v);
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileInfo fileInfo = files.get(position);
        ItemListFileSelectorBinding binding = holder.binding;
        binding.tvName.setText(fileInfo.getName());
        binding.getRoot().setSelected(fileInfo.isSelected());
        binding.tvDesc.setText(fileInfo.getFileDesc());

        String extension = fileInfo.getExtension();

        if ("apk".equals(extension)) {
            Glide.with(holder.itemView)
                    .load(fileInfo)
                    .into(binding.iconFile);
        } else if ("png".equals(extension) || "jpg".equals(extension)
                || "jpeg".equals(extension) || "webp".equals(extension)
                || "mp4".equals(extension) || "gif".equals(extension)) {
            Glide.with(holder.itemView)
                    .load(fileInfo.getUri())
                    .into(binding.iconFile);
        } else {
            binding.iconFile.setImageDrawable(null);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        }
        Object type = payloads.get(0);
        if (type.equals(1)) {
            holder.itemView.setSelected(files.get(position).isSelected());
        }
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public FileInfo getItem(int position) {
        return files.get(position);
    }

    private final Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            if (constraint == null || constraint.length() == 0) {
                filterResults.values = sources;
            } else {
                List<FileInfo> filterList = new ArrayList<>();
                for (FileInfo source : sources) {
                    if (source.getName().toLowerCase().contains(constraint.toString().toLowerCase())) {
                        filterList.add(source);
                    }
                }
                filterResults.values = filterList;
            }
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            files = (List<FileInfo>) results.values;
            notifyDataSetChanged();

        }
    };

    @Override
    public Filter getFilter() {
        return filter;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ItemListFileSelectorBinding binding;

        public ViewHolder(ItemListFileSelectorBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
