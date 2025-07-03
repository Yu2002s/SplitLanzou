package com.lanzou.cloud.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.cloud.data.LanzouFile;
import com.lanzou.cloud.databinding.ItemListRecycleFileBinding;

import java.util.List;

public class RecycleFileAdapter extends RecyclerView.Adapter<RecycleFileAdapter.ViewHolder> {

    public RecycleFileAdapter(List<LanzouFile> files) {
        this.files = files;
    }

    private List<LanzouFile> files;


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemListRecycleFileBinding binding = ItemListRecycleFileBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public static final class ViewHolder extends RecyclerView.ViewHolder {

        ItemListRecycleFileBinding binding;

        public ViewHolder(ItemListRecycleFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
