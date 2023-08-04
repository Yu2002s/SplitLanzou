package com.lanzou.split.network;

import android.content.ContentValues;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.WithHint;

import com.google.gson.GsonBuilder;
import com.lanzou.split.LanzouApplication;
import com.lanzou.split.data.LanzouDownloadResponse;
import com.lanzou.split.data.LanzouFile;
import com.lanzou.split.data.LanzouFileResponse;
import com.lanzou.split.data.LanzouFolder;
import com.lanzou.split.data.LanzouFolderResponse;
import com.lanzou.split.data.LanzouSimpleResponse;
import com.lanzou.split.data.LanzouUploadResponse;
import com.lanzou.split.data.LanzouUrl;
import com.lanzou.split.data.LanzouUrlResponse;
import com.lanzou.split.data.User;
import com.lanzou.split.event.OnFileIOListener;
import com.lanzou.split.service.LanzouService;
import com.lanzou.split.utils.FileUtils;

import org.litepal.LitePal;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Repository {

    private static final class RepositoryHolder {
        static final Repository instance = new Repository();
    }

    public static Repository getInstance() {
        return RepositoryHolder.instance;
    }

    private static final String[] allowUploadTypes = {
            "doc", "docx", "zip", "rar", "apk", "ipa", "txt", "exe",
            "7z", "e", "z", "ct", "ke", "db", "tar", "pdf",
            "w3xepub", "mobi", "azw", "azw3", "osk", "osz", "xpa", "cpk",
            "lua", "jar", "dmg", "ppt", "pptx", "xls", "xlsx", "mp3", "ipa",
            "iso", "img", "gho", "ttf", "ttc", "txf", "dwg", "bat",
            "dll", "crx", "xapk", "rp", "rpm", "rplib",
            "appimage", "lolgezi", "flac", "cad", "hwt", "accdb", "ce", "xmind", "enc",
            "bds", "bdi", "ssd", "it"
    };

    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 10; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.162 Mobile Safari/537.36 Edg/111.0.0.0";
    private static final String ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9";

    private static final String ACCEPT_LANGUAGE = "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6";

    private static final Pattern filePattern = Pattern
            .compile("(.+)\\.([a-zA-Z]+\\d?)\\.apk");

    private static final Pattern splitFilePattern = Pattern
            .compile("(.+)\\.([a-zA-Z]+\\d?)\\[(\\d+)]\\.enc");

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .addInterceptor(new Interceptor() {
                @NonNull
                @Override
                public okhttp3.Response intercept(@NonNull Chain chain) throws IOException {
                    Request request = chain.request();
                    String cookie = request.header("Cookie");
                    if (cookie == null && isLogin()) {
                        request = request.newBuilder()
                                .header("Cookie", user.getCookie())
                                .build();
                    }
                    okhttp3.Response response = chain.proceed(request);
                    String location = response.header("Location");
                    if (location != null) {
                        response.close();
                        response = chain.proceed(request.newBuilder().url(location).build());
                    }
                    return response;
                }
            })
            .build();

    private final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://up.woozooo.com")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(
                    new GsonBuilder().setVersion(1.0).create()))
            .build();

    private final LanzouService lanzouService = retrofit.create(LanzouService.class);

    private User user = getSavedUser();

    public boolean isLogin() {
        return user != null;
    }

    public void logout() {
        user.delete();
        user = null;
    }

    @Nullable
    public User getSavedUser() {
        return LitePal.where("isCurrent = ?", "1").findFirst(User.class);
    }

    public List<User> getSavedUserList() {
        return LitePal.findAll(User.class);
    }

    public void saveOrUpdateUser(User user) {
        updateCurrentUser();
        user.saveOrUpdate("uid = ?", String.valueOf(user.getUid()));
        this.user = user;
    }

    private void updateCurrentUser() {
        ContentValues contentValues = new ContentValues();
        contentValues.put("isCurrent", false);
        LitePal.updateAll(User.class, contentValues);
    }

    public void selectUser(User user) {
        updateCurrentUser();
        user.setCurrent(true);
        user.update();
        this.user = user;
    }

    @Nullable
    public String getCookie() {
        return user.getCookie();
    }

    @Nullable
    public List<LanzouFile> getFiles(long folderId, int page) {
        if (!isLogin()) {
            return null;
        }
        try {
            List<LanzouFile> lanzouFiles = new ArrayList<>();
            if (page == 1) {
                List<LanzouFile> files = getLanzouFiles(47, folderId, page);
                if (files != null) {
                    lanzouFiles.addAll(files);
                }
            }
            List<LanzouFile> files = getLanzouFiles(5, folderId, page);
            if (files != null) {
                for (LanzouFile file : files) {
                    if (file.getExtension().equals("apk")) {
                        getRealFile(file);
                    } else if (file.getExtension().equals("enc")) {
                        getRealSplitFile(file);
                    }
                }
                lanzouFiles.addAll(files);
            }
            return lanzouFiles;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<LanzouFile> getLanzouFiles(int task, long folderId, int page) throws IOException {
        Call<LanzouFileResponse> call = lanzouService.getFiles(user.getUid(), task, folderId, page);
        Response<LanzouFileResponse> response = call.execute();
        LanzouFileResponse lanzouFileResponse = response.body();
        if (lanzouFileResponse != null && lanzouFileResponse.getStatus() == 1) {
            return lanzouFileResponse.getFiles();
        }
        return null;
    }

    private void getRealFile(LanzouFile file) {
        Matcher matcher = filePattern.matcher(file.getName_all());
        if (matcher.find()) {
            String name = matcher.group(1);
            String ext = matcher.group(2);
            file.setExtension(ext);
            file.setName_all(name + "." + ext);
        }
    }

    @Nullable
    public String getRealFileName(String name) {
        Matcher matcher = filePattern.matcher(name);
        if (matcher.find()) {
            return matcher.group(1) + "." + matcher.group(2);
        }
        return null;
    }

    public Matcher getSplitFileMatcher(String name) {
        return splitFilePattern.matcher(name);
    }

    public boolean isSplitFile(String name) {
        return getSplitFileMatcher(name).find();
    }

    private void getRealSplitFile(LanzouFile file) {
        Matcher matcher = splitFilePattern.matcher(file.getName_all());
        if (matcher.find()) {
            String name = matcher.group(1);
            String size = matcher.group(3);
            String ext = matcher.group(2);
            if (size != null) {
                file.setSize(FileUtils.toSize(Long.parseLong(size)));
            }
            file.setExtension(ext);
            file.setName_all(name + "." + ext);
        }
    }

    @Nullable
    public LanzouUploadResponse.UploadInfo uploadFile(File file, Long folderId) {
        return uploadFile(file, folderId, null);
    }

    @Nullable
    public LanzouUploadResponse.UploadInfo uploadFile(File file, Long folderId,
                                                      @Nullable OnFileIOListener listener) {
        return uploadFile(file, folderId, listener, null);
    }

    @Nullable
    public LanzouUploadResponse.UploadInfo uploadFile(File file, Long folderId,
                                                      @Nullable OnFileIOListener listener, @Nullable String uploadName) {
        String fileName = uploadName == null ? file.getName() : uploadName;
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        boolean flag = false;
        for (String allowUploadType : allowUploadTypes) {
            if (allowUploadType.equals(extension)) {
                flag = true;
                break;
            }
        }
        if (!flag) {
            fileName += ".apk";
        }
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (mimeType == null) {
            mimeType = "*/*";
        }
        Log.d("jdy", "sourceFile: " + file + ", fileName: " + fileName);
        RequestBody requestBody = RequestBody.create(MediaType.parse(mimeType), file);
        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("task", "1")
                .addFormDataPart("folder_id", String.valueOf(folderId))
                .addFormDataPart("upload_file", fileName, requestBody)
                .build();
        FileRequestBody fileRequestBody = new FileRequestBody(multipartBody, listener);
        LanzouUploadResponse response = get(lanzouService.uploadFile(fileRequestBody));
        Log.d("jdy", "uploadInfo: " + response);
        if (response != null && response.getStatus() == 1) {
            return response.getUploadInfos().get(0);
        }
        return null;
    }

    @Nullable
    public String getDownloadUrl(String url, @Nullable String pwd) {
        // 获取下载地址
        try {
            if (!url.contains("/tp/")) {
                url = url.replace(".com", ".com/tp");
            }
            String html = getHtml(url);
            if (TextUtils.isEmpty(pwd)) {
                // 无密码
                Pattern pattern = Pattern.compile("submit.href = (.+) \\+ (.+)");
                Matcher matcher = pattern.matcher(html);
                if (matcher.find()) {
                    String param = matcher.group(2);
                    String word = matcher.group(1).trim();
                    Pattern pattern2 = Pattern.compile("var " + word + " = '(.+)';");
                    Pattern pattern3 = Pattern.compile("var " + param + " = '(.+)';");
                    Matcher matcher2 = pattern2.matcher(html);
                    Matcher matcher3 = pattern3.matcher(html);
                    if (matcher2.find() && matcher3.find()) {
                        String fileHost = matcher2.group(1);
                        String downloadUrl = matcher3.group(1);
                        return fileHost + downloadUrl;
                    }
                }
            } else {
                // 有密码
                String host = url.substring(0, url.indexOf(".com/") + 5);
                String requestUrl = host + "ajaxm.php";
                Pattern pattern = Pattern.compile("var postsign = '(.*?)';");
                Matcher matcher = pattern.matcher(html);
                if (!matcher.find()) {
                    throw new IllegalStateException("获取资源出错了");
                }
                FormBody formBody = new FormBody.Builder()
                        .add("action", "downprocess")
                        .add("sign", String.valueOf(matcher.group(1)))
                        .add("p", pwd)
                        .build();
                LanzouDownloadResponse responseResponse = get(lanzouService
                        .getDownloadUrl(USER_AGENT, url, requestUrl, formBody));
                if (responseResponse == null) {
                    throw new NullPointerException("获取资源失败");
                }
                if (responseResponse.getStatus() == 1) {
                    return responseResponse.getDom() + "/file/" + responseResponse.getUrl();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public List<LanzouFolder> getAllFolder() {
        LanzouFolderResponse lanzouFolderResponse = get(lanzouService.getAllFolder(19));
        if (lanzouFolderResponse != null && lanzouFolderResponse.getStatus() == 1) {
            return lanzouFolderResponse.getFolders();
        }
        return null;
    }

    @Nullable
    public LanzouUrl getLanzouUrl(long fileId) {
        LanzouUrlResponse lanzouUrlResponse = get(lanzouService.getShareUrl(22, fileId));
        if (lanzouUrlResponse != null && lanzouUrlResponse.getStatus() == 1) {
            return lanzouUrlResponse.getInfo();
        }
        return null;
    }

    @Nullable
    public Long createFolder(long id, String name, String desc) {
        LanzouSimpleResponse response = get(lanzouService.createFolder(2, id, name, desc));
        if (response != null && response.getStatus() == 1) {
            return Long.parseLong(response.getText());
        }
        return null;
    }

    public LanzouSimpleResponse deleteFile(LanzouFile lanzouFile) {
        long id = lanzouFile.isFolder() ? lanzouFile.getFolderId() : lanzouFile.getFileId();
        return deleteFile(id, !lanzouFile.isFolder());
    }

    public LanzouSimpleResponse deleteFile(long id) {
        return deleteFile(id, true);
    }

    public LanzouSimpleResponse deleteFile(long id, boolean isFile) {
        Map<String, String> map = new ArrayMap<>();
        if (isFile) {
            map.put("task", "6");
            map.put("file_id", String.valueOf(id));
        } else {
            map.put("task", "3");
            map.put("folder_id", String.valueOf(id));
        }
        return get(lanzouService.deleteFile(map));
    }

    public LanzouSimpleResponse moveFile(long fileId, long targetFolder) {
        return get(lanzouService.moveFile(20, fileId, targetFolder));
    }

    public okhttp3.Response getResponse(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Accept", ACCEPT)
                .addHeader("Accept-Language", ACCEPT_LANGUAGE)
                .build();
        return okHttpClient.newCall(request).execute();
    }

    public okhttp3.Response getRangeResponse(String url, long start) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Range", "bytes=" + start + "-")
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Accept", ACCEPT)
                .addHeader("Accept-Language", ACCEPT_LANGUAGE)
                .build();
        return okHttpClient.newCall(request).execute();
    }

    private String getHtml(String url) throws Exception {
        okhttp3.Response response = getResponse(url);
        ResponseBody body = response.body();
        assert body != null;
        return body.string();
    }

    @Nullable
    public String getLocationUrl(String url) throws IOException {
        okhttp3.Response response = getResponse(url);
        final String location = response.header("Location");
        response.close();
        return location;
    }

    @Nullable
    public Long getUploadPath() {
        return user == null ? null : user.getUploadPath();
    }

    public void updateUploadPath(long id) {
        if (user == null) {
            return;
        }
        user.setUploadPath(id);
        user.update();
    }

    @Nullable
    private <T> T get(Call<T> call) {
        try {
            return call.execute().body();
        } catch (Exception e) {
            Log.e("jdy", e.toString());
            return null;
        }
    }

}
