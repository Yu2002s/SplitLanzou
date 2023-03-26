package com.lanzou.split.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.split.data.LanzouFile;
import com.lanzou.split.databinding.ItemListFileBinding;
import com.lanzou.split.event.OnItemClickListener;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private final List<LanzouFile> lanzouFiles;

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public FileAdapter(List<LanzouFile> lanzouFiles) {
        this.lanzouFiles = lanzouFiles;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemListFileBinding binding = ItemListFileBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);
        FileViewHolder fileViewHolder = new FileViewHolder(binding);
        binding.getRoot().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(fileViewHolder.getAdapterPosition(), v);
            }
        });
        return fileViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        LanzouFile lanzouFile = lanzouFiles.get(position);
        ItemListFileBinding binding = holder.binding;
        if (lanzouFile.isFolder()) {
            binding.tvDesc.setVisibility(View.GONE);
            binding.tvName.setText(lanzouFile.getName());
        } else {
            binding.tvName.setText(lanzouFile.getName_all());
            binding.tvDesc.setText(lanzouFile.getTime() + " - " + lanzouFile.getSize()
                    + " - " + lanzouFile.getDownloadCount() + "次下载");
            binding.tvDesc.setVisibility(View.VISIBLE);
        }


    }

    @Override
    public int getItemCount() {
        return lanzouFiles.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {

        ItemListFileBinding binding;

        public FileViewHolder(ItemListFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
