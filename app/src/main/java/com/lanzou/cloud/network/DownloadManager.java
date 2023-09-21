package com.lanzou.cloud.network;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.lanzou.cloud.data.Download;
import com.lanzou.cloud.event.OnDownloadListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DownloadManager {

    private static final String TAG = "DownloadService";

    private final ExecutorService executorService = Executors.newFixedThreadPool(16);

    private final Map<Download, Future<?>> downloadMap = new TreeMap<>();

    private final List<OnDownloadListener> downloadListeners = new ArrayList<>();

    private final Repository repository = Repository.getInstance();

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            Download download = (Download) msg.obj;
            for (OnDownloadListener downloadListener : downloadListeners) {
                downloadListener.onDownload(download);
            }
        }
    };

    private void updateDownloadStatus(Download download) {
        Message msg = new Message();
        msg.obj = download;
        mHandler.sendMessage(msg);
    }

    public void addDownloadListener(OnDownloadListener listener) {
        downloadListeners.add(listener);
    }

    public void removeDownloadListener(OnDownloadListener listener) {
        downloadListeners.remove(listener);
    }
}
