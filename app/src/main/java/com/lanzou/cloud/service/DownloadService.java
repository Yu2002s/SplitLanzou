package com.lanzou.cloud.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lanzou.cloud.data.Download;
import com.lanzou.cloud.data.LanzouUrl;
import com.lanzou.cloud.data.SplitFile;
import com.lanzou.cloud.data.Upload;
import com.lanzou.cloud.event.OnDownloadListener;
import com.lanzou.cloud.network.Repository;
import com.lanzou.cloud.utils.FileUtils;

import org.litepal.LitePal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Lanzou 文件分割下载服务
 *
 * @author Yu2002s
 */
public class DownloadService extends Service {

    private static final String TAG = "DownloadService";

    /**
     * 下载线程池
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(16);

    /**
     * 下载映射表
     */
    private final Map<String, Future<?>> downloadMap = new ArrayMap<>();

    /**
     * 下载事件监听列表
     */
    private final List<OnDownloadListener> downloadListeners = new ArrayList<>();

    /**
     * 仓库实例
     */
    private final Repository repository = Repository.getInstance();

    /**
     * 内部下载路径文件
     */
    private File downloadPath;

    /**
     * 外部下载路径文件
     */
    private File externalDownloadPath;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            Download download = (Download) msg.obj;
            // 对已添加的事件进行分发
            for (OnDownloadListener downloadListener : downloadListeners) {
                downloadListener.onDownload(download);
            }
        }
    };

    /**
     * 更新下载状态，从子线程切换到主线程中进行更新操作
     * @param download 下载信息
     */
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

    /**
     * 对下载队列进行切换暂停、继续操作
     * @param download 下载信息
     */
    public void toggleDownload(Download download) {
        if (download.isComplete()) {
            // openFile
            openFile(download);
            return;
        }

        Future<?> future = downloadMap.get(download.getUrl());
        if (future != null) {
            // 正在下载的话就停止，已停止就恢复下载
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

    /**
     * 打开已下载的文件
     * @param download 下载对象信息
     */
    private void openFile(Download download) {
        String path = download.getPath();
        if (!FileUtils.openFile(path)) {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 恢复暂停的下载
     * @param download 下载对象信息
     */
    private void resumeDownload(Download download) {
        downloadMap.put(download.getUrl(), executorService.submit(() -> {
            prepareDownload(download);
        }));
    }

    /**
     * 停止下载文件
     * @param future Future
     * @param download 下载信息
     */
    private void stopDownload(Future<?> future, Download download) {
        download.stop();
        updateDownloadStatus(download);
        future.cancel(true);
    }

    /**
     * 删除下载服务
     * @param download 下载信息
     */
    public void removeDownload(Download download) {
        LitePal.delete(Download.class, download.getId());
        download.stop();
        Future<?> future = downloadMap.get(download.getUrl());
        if (future != null) {
            future.cancel(true);
        }
        if (download.getPath() == null) return;
        File file = new File(download.getPath());
        if (file.exists()) {
            file.delete();
        }
    }

    public void addDownload(long fileId) {
        addDownload(fileId, null);
    }

    /**
     * 通过 fileId 和名称添加下载
     * @param fileId 文件 id
     * @param name 文件名称
     */
    public void addDownload(long fileId, @Nullable String name) {
        // 如果只有文件的 id，则需要先通过 id 获取到文件的分享地址
        executorService.execute(() -> {
            LanzouUrl lanzouUrl = repository.getLanzouUrl(fileId);
            if (lanzouUrl == null) {
                return;
            }
            String pwd = lanzouUrl.getHasPwd() == 1 ? lanzouUrl.getPwd() : null;
            addDownload(lanzouUrl.getHost() + "/tp/" + lanzouUrl.getFileId(), name, pwd);
        });
    }

    /**
     * 通过 url、name、pwd 添加下载
     * @param url 文件分享地址
     * @param name 文件名称
     * @param pwd 文件分享密码
     */
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
                if (download.isComplete()) {
                    mHandler.post(() -> Toast.makeText(DownloadService.this, name + "任务已存在", Toast.LENGTH_SHORT).show());
                    openFile(download);
                    return;
                }
            }
            // updateDownloadStatus(download);
            mHandler.post(() -> Toast.makeText(DownloadService.this, name + "已加入下载任务", Toast.LENGTH_SHORT).show());
            prepareDownload(download);
        }));
    }

    /**
     * 准备下载文件
     * @param download 下载信息
     */
    private void prepareDownload(Download download) {
        try {
            // 进入准备状态中...
            download.prepare();
            // 更新状态
            updateDownloadStatus(download);
            // 正式开始下载文件
            startDownload(download);
        } catch (Exception e) {
            // 这里对下载出现的异常进行处理
            Log.d(TAG, "download error: " + e);
            // InterruptedIOException 是暂停下载，对其进行排除
            if (!(e instanceof InterruptedIOException)) {
                // 下载出错了
                download.error();
                updateDownloadStatus(download);
            }
        } finally {
            // 这里需要对下载进行判断
            // 如果文件已经下载完成了，就对下载队列进行删除
            // 这里表示下载过程结束了
            if (download.isComplete()) {
                // 这里需要对文件进行移动操作
                try {
                    File target = new File(externalDownloadPath, download.getName());
                    // 忽略文件已存在
                    if (new File(downloadPath, download.getName()).renameTo(target)) {
                        download.setPath(target.getPath());
                    }
                } catch (Exception ignore) {
                    // 可能出现的一些异常，不过这里忽略
                }
                // 对当前下载队列执行删除操作
                downloadMap.remove(download.getUrl());
                // downloadMap.put(download, null);
                // 移动文件到外部储存
                updateDownloadStatus(download);

                mHandler.post(() -> Toast.makeText(this, download.getName() + "下载完成", Toast.LENGTH_SHORT).show());
            }
        }
    }

    /**
     * 开始下载文件
     * @param download 下载信息
     * @throws Exception 可能抛出的异常
     */
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

        // 获取到响应信息，注意：这里获取到的可能不是完整响应，因为需要断点续传
        Response response = repository.getRangeResponse(downloadUrl, download.getCurrent());
        // 检查是否支持断点续传
        checkRangeResponse(response, download);

        // 获取到响应体信息
        ResponseBody responseBody = response.body();
        Objects.requireNonNull(responseBody, "获取资源失败");

        // 获取文件大小
        getFileLength(responseBody, download);

        // 如果下载地址和文件名相同，就通过结果头获取文件名（可能文件名错误）
        if (download.getUrl().equals(download.getName())) {
            download.setName(getFileName(response));
        }
        InputStream inputStream = responseBody.byteStream();
        try {
            int len = inputStream.read();
            boolean isSplitFile = len == 123; // 获取起始字符是否以{开头，如果是，就可能是json
            // 直接通过文件的第一个字符判断是否是分割文件
            if (isSplitFile) {
                // 读取文件的上传内容信息，可能出现异常
                Upload upload = readJsonToUpload(inputStream, len, download);
                if (upload == null) {
                    // 可能会出现异常的问题
                    return;
                }
                // 下载信息中设置上传文件信息
                download.setUpload(upload);
                // 开始处理分割文件
                handleSplitFiles(download);
            } else {
                // 写入小于100M的文件
                writeSingleFile(download, len, inputStream);
            }
        } catch (Exception e) {
            // 这里处理异常
            e.printStackTrace();
            Log.e(TAG, "startDownloadError: " + e.getMessage());
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
            Objects.requireNonNull(upload, "upload json is null");
        } catch (Exception e) {
            Log.e(TAG, "获取上传信息失败，尝试写入文本文件");
            // 如果读取上传文件出错了，就直接下载文本内容到本地
            writeTextFileToLocal(download, json);
        }
        return upload;
    }

    /**
     * 获取文件大小
     * @param responseBody 响应体
     * @param download 下载信息
     */
    private void getFileLength(ResponseBody responseBody, Download download) {
        if (download.getLength() <= 0) {
            long fileLength = responseBody.contentLength();
            if (fileLength <= 0) {
                throw new IllegalArgumentException("response body must be nonnull.");
            }
            download.setLength(fileLength);
        }
    }

    /**
     * 小于 100 M 以下的文件下载
     *
     * @param download    下载信息
     * @param len         第一个字符
     * @param inputStream 输入流
     * @throws IOException 抛出的异常
     */
    private void writeSingleFile(Download download, int len, InputStream inputStream) throws IOException {
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
        Log.d(TAG, "download complete: current: " + current + ", progress: " + progress);
        download.setCurrent(current);
        download.setProgress(progress);
        if (progress == 100) {
            download.complete();
        }
        raf.close();
        inputStream.close();
    }

    /**
     * 处理分割文件
     *
     * @param download 下载信息
     * @throws IOException 可能抛出异常
     */
    private void handleSplitFiles(Download download) throws IOException {
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
            // 遍历全部的分割文件
            for (SplitFile splitFile : splitFiles) {
                // 过滤已经完成的
                if (splitFile.isComplete()) {
                    continue;
                }
                // 指向开始的位置
                raf.seek(splitFile.getStart() + splitFile.getByteStart());
                String fileUrl = splitFile.getUrl();
                String pwd = splitFile.getPwd();
                // 获取到分割文件的实际下载地址
                String childDownloadUrl = repository.getDownloadUrl(fileUrl, pwd);
                // 获取到请求的响应信息
                Response response = repository.getRangeResponse(childDownloadUrl, splitFile.getByteStart());
                // 判断是否支持断点续传
                if (response.header("Content-Range") == null) {
                    download.setCurrent(download.getCurrent() - splitFile.getByteStart());
                    raf.seek(splitFile.getStart());
                    splitFile.setByteStart(0);
                }
                // 响应体信息
                ResponseBody childResponseBody = response.body();
                Objects.requireNonNull(childResponseBody, "获取资源失败");
                // 开始写入到本地
                writeFileToLocal(download, splitFile, childResponseBody.byteStream(), raf);
            }
        }
    }

    /**
     * 写入文件到本地，大于100M的文件则进行分割下载
     *
     * @param download    下载信息
     * @param splitFile   单个分割文件
     * @param inputStream 文件流
     * @param raf         使用随机读写进行写入文件到本地
     * @throws IOException 读写可能出现的问题
     */
    private void writeFileToLocal(Download download,
                                  SplitFile splitFile,
                                  InputStream inputStream,
                                  RandomAccessFile raf) throws IOException {
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
    private void writeTextFileToLocal(Download download, String content) throws Exception {
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
            Log.d(TAG, download.getUrl() + "不支持 Content-Range.");
        }
    }

    /**
     * 通过响应信息获取到要下载的文件名
     * @param response 响应
     * @return 文件名称
     */
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

    /**
     * 通过 json 转换成上传对象信息
     * @param jsonContent json 文本
     * @return Upload 对象
     */
    private Upload getUploadInfo(String jsonContent) {
        Gson gson = new GsonBuilder()
                .setVersion(1.0)
                .create();
        // 直接读取上传信息
        return gson.fromJson(jsonContent, Upload.class);
    }
}
