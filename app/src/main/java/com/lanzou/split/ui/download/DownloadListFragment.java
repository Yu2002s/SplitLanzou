package com.lanzou.split.ui.download;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.split.R;
import com.lanzou.split.adapter.DownloadAdapter;
import com.lanzou.split.data.Download;
import com.lanzou.split.event.OnDownloadListener;
import com.lanzou.split.event.OnItemClickListener;
import com.lanzou.split.service.DownloadService;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

public class DownloadListFragment extends Fragment implements ServiceConnection, OnDownloadListener {

    private DownloadService downloadService;

    private final List<Download> downloadList = new ArrayList<>();

    private final DownloadAdapter downloadAdapter = new DownloadAdapter(downloadList);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireContext().bindService(new Intent(requireContext(), DownloadService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        downloadService = ((DownloadService.DownloadBinder) service).getService();
        downloadService.addDownloadListener(this);
        // downloadList.addAll(downloadService.getDownloadList());
        downloadList.addAll(LitePal.order("id desc").limit(1000).find(Download.class));
        downloadAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDownload(Download download) {
        if (download.isInsert()) {
            downloadList.add(0, download);
            downloadAdapter.notifyItemInserted(0);
            return;
        }
        int index = downloadList.indexOf(download);
        if (index != -1) {
            if (downloadList.get(index) != download) {
                downloadList.set(index, download);
            }
            downloadAdapter.notifyItemChanged(index, 0);
        } else {
            downloadList.add(0, download);
            downloadAdapter.notifyItemInserted(0);
        }
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
        recyclerView.setAdapter(downloadAdapter);

        downloadAdapter.setToggleTransmissionListener((position, view1)
                -> downloadService.toggleDownload(downloadList.get(position)));

        ActivityResultLauncher<Intent> launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getData() != null) {
                        int position = result.getData().getIntExtra("position", -1);
                        if (position == -1) return;
                        downloadList.remove(position);
                        downloadAdapter.notifyItemRemoved(position);
                    }
                });

        downloadAdapter.setItemClickListener((position, view12) -> {
            DownloadInfoActivity.actionStart(requireContext(), launcher, downloadList.get(position), position);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireContext().unbindService(this);
        downloadService.removeDownloadListener(this);
    }
}
