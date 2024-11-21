package com.lanzou.cloud.network;

import android.content.ContentValues;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.GsonBuilder;
import com.lanzou.cloud.data.LanzouDownloadResponse;
import com.lanzou.cloud.data.LanzouFile;
import com.lanzou.cloud.data.LanzouFileResponse;
import com.lanzou.cloud.data.LanzouFolder;
import com.lanzou.cloud.data.LanzouFolderResponse;
import com.lanzou.cloud.data.LanzouSimpleResponse;
import com.lanzou.cloud.data.LanzouUploadResponse;
import com.lanzou.cloud.data.LanzouUrl;
import com.lanzou.cloud.data.LanzouUrlResponse;
import com.lanzou.cloud.data.User;
import com.lanzou.cloud.event.OnFileIOListener;
import com.lanzou.cloud.service.LanzouService;
import com.lanzou.cloud.utils.FileUtils;

import org.litepal.LitePal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36";
    private static final String ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9";

    private static final String ACCEPT_LANGUAGE = "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6";

    private static final Pattern filePattern = Pattern
            .compile("(.+)\\.([a-zA-Z]+\\d?)\\.apk");

    private static final Pattern splitFilePattern = Pattern
            .compile("(.+)\\.([a-zA-Z]+\\d?)\\[(\\d+)]\\.enc");

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
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
        RequestBody requestBody = RequestBody.create(MediaType.parse(mimeType), file);
        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("task", "1")
                .addFormDataPart("folder_id", String.valueOf(folderId))
                .addFormDataPart("upload_file", fileName, requestBody)
                .build();
        FileRequestBody fileRequestBody = new FileRequestBody(multipartBody, listener);
        LanzouUploadResponse response = get(lanzouService.uploadFile(fileRequestBody));
        Log.d("jdy", "uploadResponse: " + response);
        if (response != null && response.getStatus() == 1) {
            return response.getUploadInfos().get(0);
        }
        return null;
    }

    @Nullable
    public String getDownloadUrl(String url, @Nullable String pwd) {
        // 改用使用接口方式获取下载地址
        /*String key = url.substring(url.lastIndexOf("/"));
        String downloadUrl = LanzouApplication.FILE_PARSE_URL + key;
        if (!TextUtils.isEmpty(pwd)) {
            downloadUrl += "@" + pwd;
        }
        Log.d("jdy", "downloadUrl: " + downloadUrl);
        return downloadUrl;*/
        // 下面方法弃用，改用上面方法进行获取下载地址
        // 获取下载地址
        try {
            if (url.contains("/tp/")) {
                url = url.replace("tp/", "");
            }
            String html = getHtml(url);
            String host = url.substring(0, url.lastIndexOf("/"));

            if (TextUtils.isEmpty(pwd)) {
                Pattern pattern = Pattern.compile("ifr2\" name=\"[0-9]{10}\" src=\"(.+)\" frameborder");
                Matcher matcher = pattern.matcher(html);
                if (matcher.find()) {
                    String location = host + matcher.group(1);
                    String downloadHtml = getHtml(location);
                    Pattern ajaxPattern = Pattern.compile("var ajaxdata = '(.+)';");
                    Matcher ajaxMatcher = ajaxPattern.matcher(downloadHtml);
                    if (!ajaxMatcher.find()) {
                        return null;
                    }
                    String ajaxData = ajaxMatcher.group(1);
                    Pattern signPattern = Pattern.compile("'sign':'(.+)','web");
                    Matcher signMatcher = signPattern.matcher(downloadHtml);
                    if (!signMatcher.find()) {
                        return null;
                    }
                    Pattern reqPattern = Pattern.compile("url : '(.+)',");
                    Matcher reqMatcher = reqPattern.matcher(downloadHtml);
                    if (!reqMatcher.find()) {
                        return null;
                    }
                    FormBody body = new FormBody.Builder()
                            .add("action", "downprocess")
                            .add("signs", ajaxData)
                            .add("sign", signMatcher.group(1))
                            .build();
                    LanzouDownloadResponse lanzouDownloadResponse = get(lanzouService.getDownloadUrl(USER_AGENT, host, host + reqMatcher.group(1), body));
                    if (lanzouDownloadResponse.getStatus() == 1) {
                        return lanzouDownloadResponse.getDom() + "/file/" + lanzouDownloadResponse.getUrl();
                    }
                    return null;
                }
            } else {
                Pattern pattern = Pattern.compile("'sign':(.+),'p'");
                Matcher matcher = pattern.matcher(html);
                if (!matcher.find()) {
                    return null;
                }
                Pattern signPattern = Pattern.compile("var " + matcher.group(1) + " = '(.*?)';");
                Matcher signMatcher = signPattern.matcher(html);
                if (!signMatcher.find()) {
                    return null;
                }
                Pattern reqPattern = Pattern.compile("url : '/ajaxm.php\\?file=(\\d+)',");
                Matcher reqMatcher = reqPattern.matcher(html);
                if (!reqMatcher.find()) {
                    return null;
                }
                FormBody body = new FormBody.Builder()
                        .add("action", "downprocess")
                        .add("sign", signMatcher.group(1))
                        .add("p", pwd)
                        .add("kd", "1")
                        .build();
                LanzouDownloadResponse lanzouDownloadResponse = get(lanzouService.getDownloadUrl(USER_AGENT, host, host + "/ajaxm.php?file="+ reqMatcher.group(1), body));
                if (lanzouDownloadResponse.getStatus() == 1) {
                    return lanzouDownloadResponse.getDom() + "/file/" + lanzouDownloadResponse.getUrl();
                }
                return null;
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
