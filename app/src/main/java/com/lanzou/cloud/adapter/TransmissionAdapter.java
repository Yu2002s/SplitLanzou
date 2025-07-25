package com.lanzou.cloud.adapter;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.cloud.R;
import com.lanzou.cloud.databinding.ItemListTransmissionBinding;
import com.lanzou.cloud.event.OnItemClickListener;

public class TransmissionAdapter extends RecyclerView.Adapter<TransmissionViewHolder> {

    private OnItemClickListener itemClickListener;

    private OnItemClickListener toggleTransmissionListener;

    private OnItemClickListener onItemCheckChangeListener;

    public void setToggleTransmissionListener(OnItemClickListener toggleUploadListener) {
        this.toggleTransmissionListener = toggleUploadListener;
    }

    public void setItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public void setOnItemCheckChangeListener(OnItemClickListener onItemCheckChangeListener) {
        this.onItemCheckChangeListener = onItemCheckChangeListener;
    }

    Drawable resume;
    Drawable pause;

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        pause = ContextCompat.getDrawable(recyclerView.getContext(), R.drawable.baseline_pause_circle_filled_24);
        resume = ContextCompat.getDrawable(recyclerView.getContext(), R.drawable.baseline_play_circle_filled_24);
    }

    @NonNull
    @Override
    public TransmissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemListTransmissionBinding binding = ItemListTransmissionBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);
        TransmissionViewHolder viewHolder = new TransmissionViewHolder(binding);
        binding.btnToggle.setOnClickListener(v ->
                toggleTransmissionListener.onItemClick(viewHolder.getAbsoluteAdapterPosition(), v));
        binding.getRoot().setOnClickListener(v ->
                itemClickListener.onItemClick(viewHolder.getAbsoluteAdapterPosition(), v));

        if (onItemCheckChangeListener != null) {
            binding.select.setOnClickListener(v ->
                    onItemCheckChangeListener.onItemClick(viewHolder.getAbsoluteAdapterPosition(), v));
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TransmissionViewHolder holder, int position) {
    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
