package com.lanzou.split.adapter;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.split.data.Download;
import com.lanzou.split.data.Upload;
import com.lanzou.split.databinding.ItemListTransmissionBinding;
import com.lanzou.split.utils.FileUtils;

import java.util.List;

public class DownloadAdapter extends TransmissionAdapter {

    private final List<Download> downloads;

    public DownloadAdapter(List<Download> downloads) {
        this.downloads = downloads;
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
        binding.tvDesc.setText(download.getStatusStr() + " " + FileUtils.toSize(download.getCurrent())
                + "/" + FileUtils.toSize(download.getLength()) + " " + FileUtils.toSize(download.getSpeed()) + "/s");
        binding.progressBar.setVisibility(download.isComplete() ? View.GONE: View.VISIBLE);
        binding.progressBar.setProgress(download.getProgress());
        binding.btnToggle.setVisibility(download.isComplete() ? View.GONE: View.VISIBLE);
        binding.btnToggle.setBackground(download.isDownload() ? pause : resume);
    }

    @Override
    public int getItemCount() {
        return downloads.size();
    }
}