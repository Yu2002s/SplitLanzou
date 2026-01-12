package com.lanzou.cloud.ui.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.cloud.R;
import com.lanzou.cloud.adapter.UploadAdapter;
import com.lanzou.cloud.data.Upload;
import com.lanzou.cloud.event.OnUploadListener;
import com.lanzou.cloud.service.UploadService;
import com.lanzou.cloud.ui.activity.UploadInfoActivity;

import java.util.ArrayList;
import java.util.List;

public class UploadListFragment extends Fragment implements ServiceConnection, OnUploadListener {

    private UploadService uploadService;

    private final List<Upload> uploadList = new ArrayList<>();

    private final UploadAdapter uploadAdapter = new UploadAdapter(uploadList);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireContext().bindService(new Intent(requireContext(), UploadService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transmission_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(uploadAdapter);

        uploadAdapter.setToggleTransmissionListener((position, view1) -> {
            Upload upload = uploadList.get(position);
            uploadService.toggleUpload(upload);
        });

        uploadAdapter.setItemClickListener((position, view2) ->
                UploadInfoActivity.start(requireContext(), uploadList.get(position)));
    }

    @Override
    public void onUpload(Upload upload) {
        if (upload.isInsert()) {
            uploadList.add(0, upload);
            uploadAdapter.notifyItemInserted(0);
            return;
        }
        int index = uploadList.indexOf(upload);
        if (index != -1) {
            uploadAdapter.notifyItemChanged(index, 1);
        } else {
            uploadList.add(0, upload);
            uploadAdapter.notifyItemInserted(0);
        }

    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        uploadService = ((UploadService.UploadBinder) service).getService();
        uploadService.addUploadListener(this);
        uploadList.addAll(uploadService.getUploadList());
        // 服务连接时对内容进行加载
        uploadAdapter.notifyDataSetChanged();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // uploadService.removeUploadListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireContext().unbindService(this);
        uploadService.removeUploadListener(this);
        uploadService = null;
    }
}
