package com.lanzou.cloud.adapter;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.lanzou.cloud.data.Download;
import com.lanzou.cloud.databinding.ItemListTransmissionBinding;
import com.lanzou.cloud.utils.FileJavaUtils;

import java.util.List;

public class DownloadAdapter extends TransmissionAdapter {

    private final List<Download> downloads;

    public DownloadAdapter(List<Download> downloads) {
        this.downloads = downloads;
    }

    @NonNull
    @Override
    public TransmissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TransmissionViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);
        viewHolder.binding.select.setVisibility(View.VISIBLE);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TransmissionViewHolder holder, int position) {
        ItemListTransmissionBinding binding = holder.binding;
        Download download = downloads.get(position);
        binding.tvName.setText(download.getName());
        updateView(binding, download);
    }

    @Override
    public void onBindViewHolder(@NonNull TransmissionViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        }
        ItemListTransmissionBinding binding = holder.binding;
        Download download = downloads.get(position);

        updateView(binding, download);
    }

    private void updateView(ItemListTransmissionBinding binding, Download download) {
        binding.tvDesc.setText(download.getStatusStr() + " " + FileJavaUtils.toSize(download.getCurrent())
                + "/" + FileJavaUtils.toSize(download.getLength()) + " " + FileJavaUtils.toSize(download.getSpeed()) + "/s");
        binding.progressBar.setVisibility(download.isComplete() ? View.GONE: View.VISIBLE);
        binding.progressBar.setProgress(download.getProgress());
        binding.btnToggle.setVisibility(download.isComplete() ? View.GONE: View.VISIBLE);
        binding.btnToggle.setBackground(download.isDownload() ? pause : resume);
        binding.select.setSelected(download.isChecked());
    }

    @Override
    public int getItemCount() {
        return downloads.size();
    }
}