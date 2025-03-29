package com.lanzou.cloud.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lanzou.cloud.R;
import com.lanzou.cloud.data.FileInfo;
import com.lanzou.cloud.databinding.ItemListFileSelectorBinding;
import com.lanzou.cloud.event.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

public class FileSelectorAdapter extends RecyclerView.Adapter<FileSelectorAdapter.ViewHolder> implements Filterable {

    private List<FileInfo> sources;

    private List<FileInfo> files;

    /**
     * 选择按钮是否可点击
     */
    private boolean isSelectButtonClickable = false;

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

    public void setSelectButtonClickable(boolean selectButtonClickable) {
        isSelectButtonClickable = selectButtonClickable;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemListFileSelectorBinding binding = ItemListFileSelectorBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);
        ViewHolder viewHolder = new ViewHolder(binding);
        View.OnClickListener listener = v -> {
            int position = viewHolder.getAbsoluteAdapterPosition();
            FileInfo fileInfo = files.get(position);
            if (fileInfo.getExtension() != null || v.getId() == R.id.select) {
                View itemView = viewHolder.itemView;
                itemView.setSelected(!itemView.isSelected());
                fileInfo.setSelected(itemView.isSelected());
            }
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position, v);
            }
        };
        viewHolder.itemView.setOnClickListener(listener);
        if (isSelectButtonClickable) {
            viewHolder.binding.select.setOnClickListener(listener);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileInfo fileInfo = files.get(position);
        String extension = fileInfo.getExtension();

        ItemListFileSelectorBinding binding = holder.binding;
        binding.tvName.setText(fileInfo.getName());
        holder.itemView.setSelected(fileInfo.isSelected());
        binding.tvDesc.setText(fileInfo.getFileDesc());

        if (extension == null) {
            binding.iconFile.setImageResource(R.drawable.baseline_folder_24);
            return;
        }

        if (isApkFile(extension)) {
            // 直接加载
            Glide.with(holder.itemView)
                    .load(fileInfo)
                    .into(binding.iconFile);
        } else if (isMediaFile(extension)) {
            Glide.with(holder.itemView)
                    .load(fileInfo.getUri())
                    .into(binding.iconFile);
        } else {
            binding.iconFile.setImageResource(R.drawable.baseline_insert_drive_file_24);
        }
    }

    private boolean isApkFile(String extension) {
        return "apk".equals(extension);
    }

    private boolean isMediaFile(String extension) {
        return "png".equals(extension) || "jpg".equals(extension)
                || "jpeg".equals(extension) || "webp".equals(extension)
                || "mp4".equals(extension) || "gif".equals(extension);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        }
        Object type = payloads.get(0);
        if (type.equals(1)) {
            FileInfo fileInfo = files.get(position);
            holder.itemView.setSelected(fileInfo.isSelected());
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

        @SuppressLint("NotifyDataSetChanged")
        @Override
        @SuppressWarnings("unchecked cast")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            files = (List<FileInfo>) results.values;
            notifyDataSetChanged();
        }
    };

    @Override
    public Filter getFilter() {
        return filter;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ItemListFileSelectorBinding binding;

        public ViewHolder(ItemListFileSelectorBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
