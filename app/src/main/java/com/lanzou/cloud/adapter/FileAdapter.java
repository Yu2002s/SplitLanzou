package com.lanzou.cloud.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.cloud.R;
import com.lanzou.cloud.data.LanzouFile;
import com.lanzou.cloud.databinding.ItemListFileBinding;
import com.lanzou.cloud.event.OnItemClickListener;
import com.lanzou.cloud.event.OnItemLongClickListener;

import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> implements Filterable {

    private final List<LanzouFile> sources;

    private List<LanzouFile> lanzouFiles;

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    private OnItemLongClickListener longClickListener;

    public void setLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public FileAdapter(List<LanzouFile> lanzouFiles) {
        this.lanzouFiles = lanzouFiles;
        this.sources = lanzouFiles;
    }

    public void notifySelect(int position) {
        notifyItemChanged(position, 0);
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemListFileBinding binding = ItemListFileBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);
        FileViewHolder fileViewHolder = new FileViewHolder(binding);
        binding.getRoot().setOnClickListener(v ->
                listener.onItemClick(fileViewHolder.getAdapterPosition(), v));
        binding.getRoot().setOnLongClickListener(v -> {
            longClickListener.onItemLongClick(fileViewHolder.getAbsoluteAdapterPosition(), v);
            return true;
        });
        return fileViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        LanzouFile lanzouFile = lanzouFiles.get(position);
        ItemListFileBinding binding = holder.binding;
        TextView name = binding.tvName;
        if (lanzouFile.isFolder()) {
            binding.tvDesc.setVisibility(View.GONE);
            name.setTextSize(16);
            name.setText(lanzouFile.getName());
        } else {
            name.setTextSize(14);
            name.setText(lanzouFile.getName_all());
            String time = lanzouFile.getTime();
            if (time.length() > 6) {
                time = time.substring(2);
            } else {
                time = time.replace(" ", "");
            }
            binding.tvDesc.setText(String.format(name.getContext().getString(R.string.file_desc),
                    time, lanzouFile.getSize().replace(" ", ""), lanzouFile.getDownloadCount()));
            binding.tvDesc.setVisibility(View.VISIBLE);
        }

        int padding = lanzouFile.isFolder() ? 38 : 24;
        binding.getRoot().setPadding(padding, padding, padding, padding);

        binding.getRoot().setSelected(lanzouFile.isSelected());
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        }
        if (payloads.get(0).equals(0)) {
            holder.binding.getRoot().setSelected(lanzouFiles.get(position).isSelected());
        }
    }

    private final Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null || constraint.toString().isEmpty()) {
                results.values = sources;
            } else {
                List<LanzouFile> filterList = new ArrayList<>();
                for (LanzouFile source : sources) {
                    if (source.getFileName().toLowerCase().contains(constraint.toString().toLowerCase())) {
                        filterList.add(source);
                    }
                }
                results.values = filterList;
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            lanzouFiles = (List<LanzouFile>) results.values;
            notifyDataSetChanged();
        }
    };

    @Override
    public Filter getFilter() {
        return filter;
    }

    public LanzouFile getItem(int position) {
        return lanzouFiles.get(position);
    }

    public void deleteItem(int position) {
        if (position == -1) {
            return;
        }
        if (sources.size() != lanzouFiles.size()) {
            lanzouFiles.remove(sources.get(position));
        }
        sources.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return lanzouFiles.size();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {

        ItemListFileBinding binding;

        public FileViewHolder(ItemListFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
