package com.lanzou.split.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.ArrayMap;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lanzou.split.data.Download;
import com.lanzou.split.data.LanzouUrl;
import com.lanzou.split.data.SplitFile;
import com.lanzou.split.data.Upload;
import com.lanzou.split.event.OnDownloadListener;
import com.lanzou.split.network.Repository;

import org.litepal.LitePal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloadService extends Service {

    private static final String TAG = "DownloadService";

    private final ExecutorService executorService = Executors.newFixedThreadPool(16);

    private final Map<String, Future<?>> downloadMap = new ArrayMap<>();

    private final List<OnDownloadListener> downloadListeners = new ArrayList<>();

    private final Repository repository = Repository.getInstance();

    private File downloadPath;

    private File externalDownloadPath;

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
        download.update();
    }

    public class DownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new DownloadBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        downloadPath = getExternalFilesDir("Download");
        externalDownloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    public void addDownloadListener(OnDownloadListener listener) {
        downloadListeners.add(listener);
    }

    public void removeDownloadListener(OnDownloadListener listener) {
        downloadListeners.remove(listener);
    }

    public void toggleDownload(Download download) {
        if (download.isComplete()) {
            // openFile
            openFile(download);
            return;
        }

        Future<?> future = downloadMap.get(download.getUrl());
        if (future != null) {
            if (download.isDownload()) {
                stopDownload(future, download);
            } else {
                resumeDownload(download);
            }
        } else {
            // 如果任务不存在
            addDownload(download.getUrl(), download.getName(), download.getPwd());
        }
    }

    private void openFile(Download download) {
        String path = download.getPath();
        if (path == null) {
            return;
        }
        File file = new File(path);
        if (file.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri = FileProvider.getUriForFile(this, "com.lanzou.split.fileProvider", file);
            String ext = download.getName().substring(download.getName().lastIndexOf(".") + 1);
            String mimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(ext);
            if (mimeType == null) {
                mimeType = "*/*";
            }
            intent.setDataAndType(uri, mimeType);
            startActivity(intent);
        } else {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
        }
    }

    private void resumeDownload(Download download) {
        downloadMap.put(download.getUrl(), executorService.submit(() -> {
            prepareDownload(download);
        }));
    }

    private void stopDownload(Future<?> future, Download download) {
        download.stop();
        updateDownloadStatus(download);
        future.cancel(true);
    }

    public void removeDownload(Download download) {
        LitePal.delete(Download.class, download.getId());
        download.stop();
        Future<?> future = downloadMap.get(download.getUrl());
        if (future != null) {
            future.cancel(true);
        }
        File file = new File(download.getPath());
        if (file.exists()) {
            file.delete();
        }
    }

    public void addDownload(long fileId) {
        addDownload(fileId, null);
    }

    public void addDownload(long fileId, @Nullable String name) {
        executorService.execute(() -> {
            LanzouUrl lanzouUrl = repository.getLanzouUrl(fileId);
            if (lanzouUrl == null) {
                return;
            }
            String pwd = lanzouUrl.getHasPwd() == 1 ? lanzouUrl.getPwd() : null;
            addDownload(lanzouUrl.getHost() + "/tp/" + lanzouUrl.getFileId(), name, pwd);
        });
    }

    public void addDownload(String url, @Nullable String name, @Nullable String pwd) {
        if (downloadMap.containsKey(url)) {
            // 任务已存在
            Log.d(TAG, "下载任务已存在");
            return;
        }
        downloadMap.put(url, executorService.submit(() -> {
            Download download;

            Download queryDownload = LitePal
                    .where("url = ?", url)
                    .findFirst(Download.class);
            if (queryDownload == null) {
                download = new Download();
                download.setUrl(url);
                download.setTime(System.currentTimeMillis());
                download.setName(name == null ? url : name);
                download.setPwd(pwd);
                if (!download.insert()) {
                    download.error();
                    updateDownloadStatus(download);
                    return;
                }
            } else {
                download = queryDownload;
            }
            // updateDownloadStatus(download);
            mHandler.post(() -> Toast.makeText(DownloadService.this, name + "已加入下载任务", Toast.LENGTH_SHORT).show());
            prepareDownload(download);
        }));
    }

    private void prepareDownload(Download download) {
        try {
            // 进入准备状态中...
            download.prepare();
            updateDownloadStatus(download);
            startDownload(download);
        } catch (Exception e) {
            if (!(e instanceof InterruptedIOException)) {
                // 下载出错了
                download.error();
                updateDownloadStatus(download);
            }
        } finally {
            // always
            // 这里需要对下载进行判断
            // 如果文件已经下载完成了，就对下载队列进行删除
            // 这里表示上传过程结束了
            if (download.isComplete()) {
                // 这里需要对文件进行移动操作
                try {
                    File target = new File(externalDownloadPath, download.getName());
                    // ignore file exists 忽略文件已存在
                    if (new File(downloadPath, download.getName()).renameTo(target)) {
                        download.setPath(target.getPath());
                    }
                } catch (Exception ignore) {
                }
                downloadMap.remove(download.getUrl());
                // downloadMap.put(download, null);
                // 移动文件到外部储存
                updateDownloadStatus(download);

                mHandler.post(() -> Toast.makeText(this, download.getName() + "下载完成", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void startDownload(Download download) throws Exception {
        if (download.getUpload() != null) {
            // 表示已经获取到了文件信息，此时已知文件是分割文件
            handleSplitFiles(download);
            return;
        }
        String url = download.getUrl();
        // 获取下载地址
        String downloadUrl = repository.getDownloadUrl(url, download.getPwd());
        Objects.requireNonNull(downloadUrl, "获取下载地址失败");

        Response response = repository.getRangeResponse(downloadUrl, download.getCurrent());
        checkRangeResponse(response, download);

        ResponseBody responseBody = response.body();
        Objects.requireNonNull(responseBody, "获取资源失败");

        getFileLength(responseBody, download);

        // 如果下载地址和文件名相同，就通过结果头获取文件名（可能文件名错误）
        if (download.getUrl().equals(download.getName())) {
            download.setName(getFileName(response));
        }
        InputStream inputStream = responseBody.byteStream();
        try {
            int len = inputStream.read();
            boolean isSplitFile = len == 123; // 获取起始字符是否以{开头，如果是，就可能是json
            if (isSplitFile) {
                Upload upload = readJsonToUpload(inputStream, len, download);
                if (upload == null) {
                    return;
                }
                download.setUpload(upload);
                // 开始处理分割文件
                handleSplitFiles(download);
            } else {
                // 写入小于100M的文件
                writeSingleFile(download, len, inputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 这里在最后进行资源释放
            responseBody.close();
            response.close();
        }
    }

    /**
     * 读取json转换为上传文件信息
     *
     * @param inputStream 文件输入
     * @param len         第一个字节
     * @param download    下载信息
     * @return 上传文件信息
     * @throws Exception 读取文件出现的异常
     */
    private Upload readJsonToUpload(InputStream inputStream, int len, Download download) throws Exception {
        // 正在读取分割文件信息
        download.read();
        updateDownloadStatus(download);
        StringBuilder builder = download.getUploadJson() == null
                ? new StringBuilder() : download.getUploadJson();
        builder.append((char) len);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            builder.append(line).append("\n");
        }
        String json = builder.toString();
        bufferedReader.close();
        inputStream.close();
        Upload upload = null;
        try {
            upload = getUploadInfo(json);
            Log.d(TAG, "upload: " + upload);
            Objects.requireNonNull(upload, "upload json is null");
        } catch (Exception e) {
            // 如果读取上传文件出错了，就直接下载文本内容到本地
            writeCharFileToLocal(download, json);
        }
        return upload;
    }

    private void getFileLength(ResponseBody responseBody, Download download) {
        if (download.getLength() <= 0) {
            long fileLength = responseBody.contentLength();
            if (fileLength <= 0) {
                throw new IllegalArgumentException("response body must be null.");
            }
            download.setLength(fileLength);
        }
    }

    private void writeSingleFile(Download download, int len, InputStream inputStream) throws Exception {
        // 进入下载文件状态
        download.progress();
        File file = new File(downloadPath, download.getName());
        download.setPath(file.getPath());
        RandomAccessFile raf = new RandomAccessFile(file, "rwd");
        raf.seek(download.getCurrent());
        raf.write(len);
        byte[] bytes = new byte[8 * 1024];
        long start = System.currentTimeMillis();
        long current = download.getCurrent() + 1;
        long size = current;
        int progress = download.getProgress();
        while ((len = inputStream.read(bytes)) != -1) {
            raf.write(bytes, 0, len);
            current += len;
            progress = (int) (current * 100 / download.getLength());
            download.setCurrent(current);
            download.setProgress(progress);
            long now = System.currentTimeMillis();
            if (now - start >= 950) {
                start = now;
                download.setSpeed((int) (current - size));
                size = current;
                updateDownloadStatus(download);
            }
        }
        Log.d("jdy", "complete: current: " + current + ", progress: " + progress);
        download.setCurrent(current);
        download.setProgress(progress);
        if (progress == 100) {
            download.complete();
        }
        raf.close();
        inputStream.close();
    }

    private void handleSplitFiles(Download download) throws Exception {
        Upload upload = download.getUpload();
        download.setLength(upload.getLength());
        File file = new File(downloadPath, upload.getName());
        download.setPath(file.getPath());
        List<SplitFile> splitFiles = upload.getFiles();
        if (splitFiles == null || splitFiles.isEmpty()) {
            throw new IllegalStateException("资源异常");
        }
        // 进入下载文件状态
        download.progress();
        try (RandomAccessFile raf = new RandomAccessFile(file, "rwd")) {
            raf.setLength(upload.getLength());
            for (SplitFile splitFile : splitFiles) {
                if (splitFile.isComplete()) {
                    continue;
                }
                raf.seek(splitFile.getStart() + splitFile.getByteStart());
                String fileUrl = splitFile.getUrl();
                String pwd = splitFile.getPwd();
                String childDownloadUrl = repository.getDownloadUrl(fileUrl, pwd);
                Response response = repository.getRangeResponse(childDownloadUrl, splitFile.getByteStart());
                if (response.header("Content-Range") == null) {
                    download.setCurrent(download.getCurrent() - splitFile.getByteStart());
                    raf.seek(splitFile.getStart());
                    splitFile.setByteStart(0);
                }
                ResponseBody childResponseBody = response.body();
                Objects.requireNonNull(childResponseBody, "获取资源失败");
                writeFileToLocal(download, splitFile, childResponseBody.byteStream(), raf);
            }
        }
    }

    /**
     * 大于100M的文件则进行分割下载
     *
     * @param download    下载信息
     * @param splitFile   单个分割文件
     * @param inputStream 文件流
     * @param raf         使用随机读写进行写入文件到本地
     * @throws Exception 读写可能出现的问题
     */
    private void writeFileToLocal(Download download,
                                  SplitFile splitFile,
                                  InputStream inputStream,
                                  RandomAccessFile raf) throws Exception {
        // 这里对文件进行下载到本地
        byte[] bytes = new byte[1024 * 8];
        int len;
        long start = System.currentTimeMillis();
        int progress = download.getProgress();
        long current = download.getCurrent();
        long size = 0;
        while ((len = inputStream.read(bytes)) != -1) {
            raf.write(bytes, 0, len);
            current += len;
            splitFile.setByteStart(splitFile.getByteStart() + len);
            progress = (int) (current * 100 / download.getLength());
            download.setCurrent(current);
            download.setProgress(progress);
            long now = System.currentTimeMillis();
            if (now - start >= 950) {
                start = now;
                download.setSpeed((int) (current - size));
                size = current;
                updateDownloadStatus(download);
            }
        }
        download.setProgress(progress);
        download.setCurrent(current);

        // 当进度为100时则表示已下载完成了
        if (progress == 100) {
            download.complete();
        }
    }

    /**
     * 写入文本文件到本地
     *
     * @param download 下载信息
     * @param content  文本内容
     * @throws Exception 读写出现的异常
     */
    private void writeCharFileToLocal(Download download, String content) throws Exception {
        File file = new File(downloadPath, download.getName());
        download.setPath(file.getPath());
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(content);
        writer.close();
        download.setCurrent(download.getLength());
        download.setProgress(100);
        download.complete();
    }

    /**
     * 检查是否支持断点下载
     *
     * @param response 结果头
     * @param download 下载信息
     */
    private void checkRangeResponse(Response response, Download download) {
        if (response.header("Content-Range") == null) {
            download.setCurrent(0);
            download.setProgress(0);
            Log.d("jdy", "range false");
        }
    }

    private String getFileName(Response response) {
        String header = response.header("Content-Disposition");
        // attachment; filename= open-install-2.4.6-I602.exe
        if (header != null) {
            int index = header.indexOf("=") + 2;
            String s = URLDecoder.decode(header.substring(index));
            String fileName = repository.getRealFileName(s);
            return fileName == null ? s : fileName;
        }
        return String.valueOf(System.currentTimeMillis());
    }

    private Upload getUploadInfo(String jsonContent) {
        Gson gson = new GsonBuilder()
                .setVersion(1.0)
                .create();
        // 直接读取上传信息
        return gson.fromJson(jsonContent, Upload.class);
    }
}
