package com.lanzou.cloud.adapter;

import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.cloud.databinding.ItemListTransmissionBinding;

class TransmissionViewHolder extends RecyclerView.ViewHolder {
    ItemListTransmissionBinding binding;

    public TransmissionViewHolder(ItemListTransmissionBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}
