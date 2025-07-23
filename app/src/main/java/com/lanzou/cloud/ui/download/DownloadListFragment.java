package com.lanzou.cloud.ui.download;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.cloud.R;
import com.lanzou.cloud.adapter.DownloadAdapter;
import com.lanzou.cloud.data.Download;
import com.lanzou.cloud.data.Upload;
import com.lanzou.cloud.event.OnDownloadListener;
import com.lanzou.cloud.service.DownloadService;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DownloadListFragment extends Fragment implements ServiceConnection, OnDownloadListener, MenuProvider {

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

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        downloadService = ((DownloadService.DownloadBinder) service).getService();
        downloadService.addDownloadListener(this);
        // downloadList.addAll(downloadService.getDownloadList());
        List<Download> downloads = LitePal.order("id desc")
                .limit(1000).find(Download.class, true);
        downloads.forEach(download -> {
            if (download.isDownload() && !downloadService.isDownloading(download)) {
                download.setStatus(Upload.STOP);
            }
        });
        downloadList.addAll(downloads);
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

    private void deleteFiles(List<Download> downloads) {
        if (downloads.isEmpty()) {
            Toast.makeText(requireContext(), "请选择文件", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            int size = downloads.size();
            for (int i = size - 1; i >= 0; i--) {
                Download download = downloads.get(i);
                downloadService.removeDownload(download);
                int index = -1;
                for (int j = 0; j < downloadList.size(); j++) {
                    Download localDownload = downloadList.get(j);
                    if (download.getId() == localDownload.getId()) {
                        index = j;
                        break;
                    }
                }
                if (index == -1) {
                    index = downloadList.indexOf(download);
                }
                final int position = index;
                requireActivity().runOnUiThread(() -> {
                    downloadList.remove(position);
                    downloadAdapter.notifyItemRemoved(position);
                });
            }
        }).start();
    }

    private void clearFiles() {
        new Thread(new Runnable() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void run() {
                downloadService.removeAllDownload();
                downloadList.clear();
                requireActivity().runOnUiThread(downloadAdapter::notifyDataSetChanged);
            }
        }).start();
    }

    private void deleteFiles() {
        List<Download> downloads = downloadList.stream()
                .filter(Download::isChecked).collect(Collectors.toList());
        deleteFiles(downloads);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transmission_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner());
        RecyclerView recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(downloadAdapter);

        downloadAdapter.setToggleTransmissionListener((position, view1)
                -> downloadService.toggleDownload(downloadList.get(position)));

        downloadAdapter.setOnItemCheckChangeListener((position, view2) -> {
            Download download = downloadList.get(position);
            download.setChecked(!download.isChecked());
            downloadAdapter.notifyItemChanged(position, 0);
        });

        ActivityResultLauncher<Intent> launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getData() != null) {
                        int position = result.getData().getIntExtra("position", -1);
                        if (position == -1) return;
                        downloadList.remove(position);
                        downloadAdapter.notifyItemRemoved(position);
                    }
                });

        downloadAdapter.setItemClickListener((position, view12) ->
                DownloadInfoActivity.actionStart(requireContext(), launcher, downloadList.get(position), position));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireContext().unbindService(this);
        downloadService.removeDownloadListener(this);
    }

    @Override
    public void onCreateMenu(@org.jspecify.annotations.NonNull Menu menu, @org.jspecify.annotations.NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_download, menu);
        MenuItem menuItem = menu.findItem(R.id.delete_download);
        menuItem.setOnMenuItemClickListener(item -> {
            deleteFiles();
            return true;
        });
        menu.findItem(R.id.clear_download).setOnMenuItemClickListener(item -> {
            clearFiles();
            return true;
        });
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onMenuItemSelected(@org.jspecify.annotations.NonNull MenuItem menuItem) {
        return true;
    }
}
