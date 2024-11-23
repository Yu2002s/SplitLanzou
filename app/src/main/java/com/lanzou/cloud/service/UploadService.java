package com.lanzou.cloud.service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lanzou.cloud.LanzouApplication;
import com.lanzou.cloud.data.LanzouPage;
import com.lanzou.cloud.data.LanzouUploadResponse;
import com.lanzou.cloud.data.LanzouUrl;
import com.lanzou.cloud.data.SplitFile;
import com.lanzou.cloud.data.Upload;
import com.lanzou.cloud.event.OnFileIOListener;
import com.lanzou.cloud.event.OnUploadListener;
import com.lanzou.cloud.network.Repository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import ando.file.core.FileUri;

public class UploadService extends Service {

    private final ExecutorService executor = Executors.newFixedThreadPool(16);

    private static final int MAX_UPLOAD_SIZE = 99 * 1024 * 1024;

    /**
     * 分割文件后缀标识
     */
    public static final String SPLIT_FILE_EXTENSION = "enc";

    private File cacheFile;

    private final Repository repository = Repository.getInstance();

    private final Map<Upload, Future<?>> uploadMap = new TreeMap<>();

    private final List<OnUploadListener> uploadListeners = new ArrayList<>();

    public void addUploadListener(OnUploadListener listener) {
        uploadListeners.add(listener);
    }

    public void removeUploadListener(OnUploadListener listener) {
        uploadListeners.remove(listener);
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            Upload upload = (Upload) msg.obj;
            for (OnUploadListener uploadListener : uploadListeners) {
                uploadListener.onUpload(upload);
            }
        }
    };

    public class UploadBinder extends Binder {
        public UploadService getService() {
            return UploadService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new UploadBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        cacheFile = new File(getExternalCacheDir(), "uploads");
        if (!cacheFile.exists()) {
            cacheFile.mkdir();
        }
    }

    public List<Upload> getUploadList() {
        return new ArrayList<>(uploadMap.keySet());
    }

    private void updateUploadStatus(Upload upload) {
        // 更新上传状态
        Message msg = new Message();
        msg.obj = upload;
        mHandler.sendMessage(msg);
    }

    public void toggleUpload(Upload upload) {
        if (upload.isComplete()) {
            // openFile
            // do nothing
            return;
        }
        Set<Map.Entry<Upload, Future<?>>> entries = uploadMap.entrySet();
        for (Map.Entry<Upload, Future<?>> entry : entries) {
            Upload uploadTask = entry.getKey();
            if (uploadTask.equals(upload)) {
                if (uploadTask.isUpload()) {
                    Future<?> future = entry.getValue();
                    stopUpload(future, uploadTask);
                    Log.d("jdy", "stopUpload");
                } else {
                    if (uploadTask.isComplete()) {
                        return;
                    }
                    resumeUpload(uploadTask);
                    Log.d("jdy", "reUpload");
                }
                break;
            }
        }
    }

    private void resumeUpload(Upload upload) {
        upload.setProgress(0);
        upload.setCurrent(0);
        uploadMap.put(upload, prepareUpload(upload));
    }

    private void stopUpload(Future<?> future, Upload upload) {
        upload.stop();
        updateUploadStatus(upload);
        // 停止上传
        if (future.cancel(true)) {
            Log.d("jdy", "stop upload success");
        } else {
            Log.e("jdy", "stop upload error");
        }
    }

    public void uploadFile(String path, LanzouPage lanzouPage) {
        // 生成上传文件信息
        Upload upload = new Upload();
        upload.setPath(path);
        upload.setTime(System.currentTimeMillis());
        // 检查所有上传任务中是否存在
        if (uploadMap.containsKey(upload)) {
            Toast.makeText(this, "上传任务已存在", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = path.substring(path.lastIndexOf("/") + 1);
        if (name.equals("base.apk")) {
            PackageManager packageManager = LanzouApplication.context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageArchiveInfo(path, 0);
            upload.setName(packageInfo.applicationInfo.loadLabel(packageManager).toString() + "-" + packageInfo.versionName + ".apk");
        } else {
            upload.setName(name);
        }
        upload.setUploadPage(lanzouPage);
        // insert upload
        // uploadList.add(upload);
        upload.insert();
        updateUploadStatus(upload);
        Toast.makeText(this, name + "已加入上传队列", Toast.LENGTH_SHORT).show();
        uploadMap.put(upload, prepareUpload(upload));
    }

    private Future<?> prepareUpload(Upload upload) {
        return executor.submit(() -> {
            try {
                startUploadFile(upload);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("jdy", "uploadError: " + e.getMessage());
                // 哎呀上传出错了
                upload.error();
                updateUploadStatus(upload);
            } finally {
                // always
                // 删除队列
                if (upload.isComplete()) {
                    // uploadMap.remove(upload);
                    uploadMap.put(upload, null);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(UploadService.this, upload.getName() + "上传完成", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                Log.d("jdy", "finish Upload");
            }
        });
    }

    /**
     * 正式开始上传文件
     *
     * @param upload 上传对象
     * @throws Exception 异常
     */
    private void startUploadFile(Upload upload) throws Exception {
        String path = upload.getPath();
        File file;
        if (!path.startsWith("/")) {
            String filePath = null;
            if (path.startsWith("file://")) {
                filePath = Uri.parse(path).getPath();
            }
            if (filePath == null) {
                filePath = FileUri.INSTANCE.getPathByUri(Uri.parse(path));
            }

            if (filePath == null) {
                throw new IllegalStateException("呜呜呜呜，这个还没适配");
            }
            file = new File(filePath);
            upload.setName(file.getName());
            upload.setPath(filePath);
        } else {
            file = new File(path);
        }
        upload.setLength(file.length());
        int blockSize = (int) (upload.getLength() % MAX_UPLOAD_SIZE == 0
                ? upload.getLength() / MAX_UPLOAD_SIZE
                : upload.getLength() / MAX_UPLOAD_SIZE + 1);
        upload.setBlockSize(blockSize);
        upload.prepare();
        updateUploadStatus(upload);
        OnFileIOListener fileIOListener = createFileIOListener(upload);
        List<File> files = getSplitFiles(file, fileIOListener);
        upload.progress();
        upload.setProgress(0);
        upload.setCurrent(0);
        File targetUploadFile;
        if (files.size() == 1) {
            // 这里处理单个文件进行上传
            targetUploadFile = files.get(0);
        } else {
            getSplitUploadFiles(upload, files, new OnFileIOListener() {
                private long now = System.currentTimeMillis();
                private long size;

                @Override
                public void onProgress(long current, long length, long byteCount) {
                    upload.setCurrent(upload.getCurrent() + byteCount);
                    int progress = (int) (upload.getCurrent() * 100 / upload.getLength());
                    upload.setProgress(progress);
                    long time = System.currentTimeMillis();
                    if (time - now >= 1000) {
                        now = time;
                        int speed = (int) (upload.getCurrent() - size);
                        size = upload.getCurrent();
                        upload.setSpeed(speed);
                        updateUploadStatus(upload);
                    }
                }
            });
            targetUploadFile = createJsonFile(upload);
        }
        boolean isSplitFile = files.size() != 1;
        OnFileIOListener listener = isSplitFile ? null : createFileIOListener(upload);
        // 之后对文件进行生成
        LanzouUploadResponse.UploadInfo uploadInfo = repository
                .uploadFile(targetUploadFile, upload.getUploadPage().getFolderId(),
                        listener, isSplitFile ? null : upload.getName());
        if (files.size() != 1) {
            targetUploadFile.delete();
        }
        if (!upload.isUpload()) {
            return;
        }
        if (uploadInfo != null) {
            // 文件上传成功了
            if (upload.getProgress() != 100) {
                upload.setProgress(100);
                upload.setCurrent(upload.getLength());
            }
            upload.complete();
        } else {
            Log.d("jdy", "uploadError");
            upload.error();
        }

        updateUploadStatus(upload);
    }

    private OnFileIOListener createFileIOListener(Upload upload) {
        return new OnFileIOListener() {
            private long now;
            private long size;

            @Override
            public void onProgress(long current, long length, long byteCount) {
                long time = System.currentTimeMillis();
                if (time - now >= 200) {
                    now = time;
                    int speed = (int) (current - size);
                    size = current;
                    upload.setSpeed(speed);
                    int progress = (int) (current * 100 / length);
                    upload.setCurrent(current);
                    upload.setProgress(progress);
                    updateUploadStatus(upload);
                }
            }
        };
    }

    private File createJsonFile(Upload upload) throws IOException {
        // 最后对模板文件进行生成上传
        Gson gson = new GsonBuilder()
                .setVersion(1.0)
                .create();
        String json = gson.toJson(upload);
        // "[" + upload.getLength() + "]." + SPLIT_FILE_EXTENSION
        File jsonFile = new File(cacheFile, upload.getName() + "[" + upload.getLength() + "]." + SPLIT_FILE_EXTENSION);
        // File jsonFile = new File(cacheFile, upload.getName());
        // File jsonFile = new File(cacheFile, name + "(" + upload.getLength() + ")" + "." + ext + "." + SPLIT_FILE_EXTENSION);
        if (jsonFile.exists() && !jsonFile.delete()) {
            // 有错误
            throw new IOException("文件覆盖失败");
        }
        jsonFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(jsonFile);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        bw.write(json);
        bw.close();
        fos.close();
        return jsonFile;
    }

    private void getSplitUploadFiles(Upload upload, List<File> files, OnFileIOListener listener) {
        List<SplitFile> splitFiles = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            // 这里对基本的上传信息进行完善
            File uploadFile = files.get(i);
            long length = uploadFile.length();
            SplitFile splitFile = new SplitFile();
            splitFile.setIndex(i);
            splitFile.setStart((long) i * MAX_UPLOAD_SIZE);
            splitFile.setEnd(splitFile.getStart() + length);
            splitFile.setLength(length);
            splitFiles.add(splitFile);
        }
        if (splitFiles.isEmpty()) {
            throw new IllegalStateException("文件资源异常");
        }
        upload.setFiles(splitFiles);
        for (int i = 0; i < files.size(); i++) {
            File uploadFile = files.get(i);
            SplitFile splitFile = splitFiles.get(i);
            upload.setIndex(i);
            LanzouUploadResponse.UploadInfo uploadInfo = repository
                    .uploadFile(uploadFile, repository.getUploadPath(), listener); // 需要修改id,上传到指定的缓存文件目录
            if (uploadInfo != null) {
                // 上传成功了
                LanzouUrl lanzouUrl = repository.getLanzouUrl(uploadInfo.getId());
                if (lanzouUrl != null) {
                    splitFile.setFileId(uploadInfo.getId());
                    if (lanzouUrl.getHasPwd() == 1) {
                        splitFile.setPwd(lanzouUrl.getPwd());
                    }
                    splitFile.setUrl(lanzouUrl.getHost() + "/tp/" + lanzouUrl.getFileId());
                } else {
                    throw new NullPointerException("upload url is null.");
                }
            } else {
                throw new IllegalStateException("上传失败了，请重试");
            }
            // 结束上传后就对文件进行删除处理
            uploadFile.delete();
            if (i == files.size() - 1) {
                uploadFile.getParentFile().delete();
            }
        }

    }

    /**
     * 获取裁剪文件
     *
     * @param file 目标文件
     * @return 返回裁剪后的文件
     */
    private List<File> getSplitFiles(File file, OnFileIOListener listener) throws Exception {
        List<File> list = new ArrayList<>();
        long fileLength = file.length();
        if (fileLength < MAX_UPLOAD_SIZE) {
            // 文件太小，不进行裁剪
            list.add(file);
            return list;
        }
        String fileName = file.getName();
        int index = fileName.lastIndexOf(".");
        String name = fileName.substring(0, index);
        // String extension = fileName.substring(index + 1);
        FileInputStream fis = new FileInputStream(file);
        byte[] bytes = new byte[8 * 1024];
        long start = 0;
        int position = 0;
        while (start < fileLength) {
            File save = new File(cacheFile, fileName);
            if (!save.exists() && !save.mkdir()) {
                break;
            }
            File output = new File(save, name + "[" + position + "]" + ".apk" /*+ extension*/);
            position++;
            if (output.exists() && !output.delete()) {
                break;
            }
            if (!output.createNewFile()) {
                break;
            }
            FileOutputStream fos = new FileOutputStream(output);
            int len;
            long size = 0;
            while (size < MAX_UPLOAD_SIZE && (len = fis.read(bytes)) != -1) {
                size += len;
                start += len;
                fos.write(bytes, 0, len);
                listener.onProgress(start, fileLength, len);
            }
            fos.close();
            list.add(output);
        }
        fis.close();
        return list;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!executor.isShutdown()) {
            executor.shutdownNow();
        }
        uploadMap.clear();
        uploadListeners.clear();
        mHandler.removeCallbacksAndMessages(null);
    }
}
