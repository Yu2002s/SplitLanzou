package com.lanzou.cloud.adapter;

import android.view.View;

import androidx.annotation.NonNull;

import com.lanzou.cloud.data.Upload;
import com.lanzou.cloud.databinding.ItemListTransmissionBinding;
import com.lanzou.cloud.utils.FileJavaUtils;

import java.util.List;

public class UploadAdapter extends TransmissionAdapter {

    private final List<Upload> uploads;

    public UploadAdapter(List<Upload> uploads) {
        this.uploads = uploads;
    }

    @Override
    public void onBindViewHolder(@NonNull TransmissionViewHolder holder, int position) {
        Upload upload = uploads.get(position);
        ItemListTransmissionBinding binding = holder.binding;
        binding.tvName.setText(upload.getName());
        updateView(binding, upload);
    }

    @Override
    public void onBindViewHolder(@NonNull TransmissionViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        }
        Upload upload = uploads.get(position);
        ItemListTransmissionBinding binding = holder.binding;
        updateView(binding, upload);
    }

    private void updateView(ItemListTransmissionBinding binding, Upload upload) {
        binding.tvDesc.setText(upload.getStatusStr() + " " + FileJavaUtils.toSize(upload.getCurrent())
                + "/" + FileJavaUtils.toSize(upload.getLength()) + " " + FileJavaUtils.toSize(upload.getSpeed()) + "/s");
        binding.progressBar.setProgress(upload.getProgress());
        binding.progressBar.setVisibility(upload.isComplete() ? View.GONE: View.VISIBLE);
        binding.btnToggle.setVisibility(upload.isComplete() ? View.GONE: View.VISIBLE);
        binding.btnToggle.setBackground(upload.isUpload() ? pause : resume);
    }

    @Override
    public int getItemCount() {
        return uploads.size();
    }

}
