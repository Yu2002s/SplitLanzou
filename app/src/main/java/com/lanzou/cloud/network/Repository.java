package com.lanzou.cloud.network;

import android.content.ContentValues;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.GsonBuilder;
import com.lanzou.cloud.LanzouApplication;
import com.lanzou.cloud.data.LanzouDownloadResponse;
import com.lanzou.cloud.data.LanzouFile;
import com.lanzou.cloud.data.LanzouFileResponse;
import com.lanzou.cloud.data.LanzouFolder;
import com.lanzou.cloud.data.LanzouFolderResponse;
import com.lanzou.cloud.data.LanzouSimpleResponse;
import com.lanzou.cloud.data.LanzouTask;
import com.lanzou.cloud.data.LanzouUploadResponse;
import com.lanzou.cloud.data.LanzouUrl;
import com.lanzou.cloud.data.LanzouUrlResponse;
import com.lanzou.cloud.data.User;
import com.lanzou.cloud.event.OnFileIOListener;
import com.lanzou.cloud.service.LanzouService;
import com.lanzou.cloud.service.UploadService;
import com.lanzou.cloud.utils.FileJavaUtils;
import com.lanzou.cloud.utils.GsonConverterFactory;
import com.lanzou.cloud.utils.SSLSocketClient;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.litepal.LitePal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import retrofit2.Retrofit;

public class Repository {

    private static final class RepositoryHolder {
        static final Repository instance = new Repository();
    }

    public static Repository getInstance() {
        return RepositoryHolder.instance;
    }

    private static final String TAG = "Repository";

    private static final String[] allowUploadTypes = {
            "doc", "docx", "zip", "rar", "apk", "ipa", "txt", "exe",
            "7z", "e", "z", "ct", "ke", "db", "tar", "pdf",
            "w3xepub", "mobi", "azw", "azw3", "osk", "osz", "xpa", "cpk",
            "lua", "jar", "dmg", "ppt", "pptx", "xls", "xlsx", "mp3", "ipa",
            "iso", "img", "gho", "ttf", "ttc", "txf", "dwg", "bat",
            "dll", "crx", "xapk", "rp", "rpm", "rplib",
            "appimage", "lolgezi", "flac", "cad", "hwt", "accdb", "ce", "xmind", "enc",
            "bds", "bdi", "conf", "it"
    };

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%d.0.0.0 Safari/537.36 Edg/%d.0.0.0";
    private static final String ACCEPT = "*/*";
    private static final String ACCEPT_LANGUAGE = "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6";

    private static final Pattern filePattern = Pattern
            .compile("(.+)\\.([a-zA-Z]+\\d?)\\.apk");

    private static final Pattern splitFilePattern = Pattern
            .compile("(.+)\\.([a-zA-Z]+\\d?)\\[(\\d+)]\\." + UploadService.SPLIT_FILE_EXTENSION);

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(SSLSocketClient.getSSLSocketFactory(), SSLSocketClient.geX509tTrustManager())
            .hostnameVerifier(SSLSocketClient.getHostnameVerifier())
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

    private static final String BASE_URL = "https://up.woozooo.com";

    private static final String RECYCLE_BIN_URL = BASE_URL + "/mydisk.php?item=recycle";

    private static final String RECYCLE_BIN_FILES_URL = RECYCLE_BIN_URL + "&action=files";

    private final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
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
        return user == null ? null : user.getCookie();
    }

    @Nullable
    public List<LanzouFile> getFiles(long folderId, int page) {
        if (!isLogin()) {
            return null;
        }
        List<LanzouFile> lanzouFiles = new ArrayList<>();
        if (page == 1) {
            List<LanzouFile> files = getLanzouFiles(LanzouTask.GET_FOLDERS.id, folderId, page);
            if (files != null) {
                lanzouFiles.addAll(files);
            }
        }
        List<LanzouFile> files = getLanzouFiles(LanzouTask.GET_FILES.id, folderId, page);
        if (files != null) {
            for (LanzouFile file : files) {
                if (file.getExtension().equals("apk")) {
                    getRealFile(file);
                } else if (file.getExtension().equals(UploadService.SPLIT_FILE_EXTENSION)) {
                    getRealSplitFile(file);
                }
            }
            lanzouFiles.addAll(files);
        }
        return lanzouFiles;
    }

    private List<LanzouFile> getLanzouFiles(int task, long folderId, int page) {
        Call<LanzouFileResponse> call = lanzouService.getFiles(user.getUid(), task, folderId, page);
        LanzouFileResponse lanzouFileResponse = get(call);
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
                file.setSize(FileJavaUtils.toSize(Long.parseLong(size)));
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
        Log.d(TAG, "uploadFile: " + file);
        Log.d(TAG, "fileName: " + fileName + ", " + "extension: " + extension + ", mimeType: " + mimeType);
        RequestBody requestBody = RequestBody.create(MediaType.parse(mimeType), file);
        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("task", "1")
                .addFormDataPart("folder_id", String.valueOf(folderId))
                .addFormDataPart("upload_file", fileName, requestBody)
                .build();
        FileRequestBody fileRequestBody = new FileRequestBody(multipartBody, listener);
        LanzouUploadResponse response = get(lanzouService.uploadFile(fileRequestBody));
        Log.d(TAG, "uploadResponse: " + response);
        if (response != null && response.getStatus() == 1) {
            return response.getUploadInfos().get(0);
        }
        return null;
    }

    /**
     * 通过 url 和 pwd 获取文件的实际下载地址
     *
     * @param url 分享地址
     * @param pwd 分享密码
     * @return 实际下载地址
     */
    @Nullable
    public String getDownloadUrl(String url, @Nullable String pwd) {
        // 改用使用接口方式获取下载地址
        String key = url.substring(url.lastIndexOf("/"));
        String downloadUrl = LanzouApplication.API_URL + "/lz" + key;
        if (!TextUtils.isEmpty(pwd)) {
            downloadUrl += "@" + pwd;
        }
        return downloadUrl;
    }

    @Nullable
    @Deprecated
    public String getDownloadUrlForLocal(@NonNull String url, @Nullable String pwd) {
        Log.i(TAG, "getDownloadUrlForLocal, url:" + url + ", pwd:" + pwd);
        try {
            String html = getHtml(url + "?webtp2&error=fileid?p");
            String host = url.substring(0, url.lastIndexOf("/"));

            if (TextUtils.isEmpty(pwd)) {
                Log.i(TAG, "url: " + url + "?webtp2&error=fileid?p");
                Log.d(TAG, "html: " + html);
                Document document = Jsoup.parse(html);
                Element iframe = document.selectFirst("iframe");
                Log.d(TAG, "iframe: " + iframe);
                if (iframe == null) {
                    return null;
                }
                String location = host + iframe.attr("src");
                Log.i(TAG, "location: " + location);
                String downloadHtml = getHtml(location);
                Log.i(TAG, "downloadHtml: " + downloadHtml);
                Pattern ajaxPattern = Pattern.compile("var ajaxdata = '(.+)';");
                Matcher ajaxMatcher = ajaxPattern.matcher(downloadHtml);
                if (!ajaxMatcher.find()) {
                    return null;
                }
                String ajaxData = ajaxMatcher.group(1);
                Log.i(TAG, "ajaxData: " + ajaxData);
                Pattern signPattern = Pattern.compile("'wp_sign':'(.+)',");
                Matcher signMatcher = signPattern.matcher(downloadHtml);
                if (!signMatcher.find()) {
                    return null;
                }
                Pattern reqPattern = Pattern.compile("url : '(.+)',");
                Matcher reqMatcher = reqPattern.matcher(downloadHtml);
                if (!reqMatcher.find()) {
                    return null;
                }
                Log.i(TAG, "signs: " + ajaxData + ", sign: " + signMatcher.group(1));
                FormBody body = new FormBody.Builder()
                        .add("action", "downprocess")
                        .add("signs", ajaxData)
                        .add("sign", signMatcher.group(1))
                        .build();
                LanzouDownloadResponse lanzouDownloadResponse = get(lanzouService.getDownloadUrl(USER_AGENT, host, host + reqMatcher.group(1), body));
                if (lanzouDownloadResponse.getStatus() == 1) {
                    return lanzouDownloadResponse.getDom() + "/file/" + lanzouDownloadResponse.getUrl();
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
                LanzouDownloadResponse lanzouDownloadResponse = get(lanzouService.getDownloadUrl(USER_AGENT, host, host + "/ajaxm.php?file=" + reqMatcher.group(1), body));
                if (lanzouDownloadResponse.getStatus() == 1) {
                    return lanzouDownloadResponse.getDom() + "/file/" + lanzouDownloadResponse.getUrl();
                }
            }
            return null;
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

    @Nullable
    public LanzouUrl getFolder(long folderId) {
        LanzouUrlResponse lanzouUrlResponse = get(lanzouService.getFolder(18, folderId));
        if (lanzouUrlResponse != null && lanzouUrlResponse.getStatus() == 1) {
            return lanzouUrlResponse.getInfo();
        }
        return null;
    }

    public LanzouSimpleResponse editFolderPassword(long fileId, boolean enable, String pwd) {
        return get(lanzouService.editFolderPassword(16, fileId, enable ? 1 : 0, pwd));
    }

    public LanzouSimpleResponse editFilePassword(long fileId, boolean enable, String pwd) {
        return get(lanzouService.editFilePassword(23, fileId, enable ? 1 : 0, pwd));
    }

    public List<LanzouFile> getRecycleFiles() {
        try {
            String html = get(lanzouService.getRecycleFiles());
            if (html == null) {
                return Collections.emptyList();
            }
            Document document = Jsoup.parse(html);
            Elements elements = document.select("#container div.n1 table tr");
            if (elements.isEmpty()) {
                return Collections.emptyList();
            }
            elements.remove(0);
            elements.remove(elements.size() - 1);
            List<LanzouFile> lanzouFiles = new ArrayList<>();
            for (Element element : elements) {
                LanzouFile lanzouFile = new LanzouFile();
                Elements tds = element.select("td");
                String id = tds.get(0).selectFirst("input").attr("value");
                Element a = tds.get(0).selectFirst("a");
                boolean isFolder = a.selectFirst("img")
                        .attr("src").equals("images/folder.gif");
                String name = a.text();
                lanzouFile.setName(name);
                lanzouFile.setName_all(name);
                lanzouFile.setSize(tds.get(1).text());
                lanzouFile.setTime(tds.get(2).text());
                int index = name.lastIndexOf(".");
                String extension = null;
                if (index != -1) {
                    extension = name.substring(index + 1);
                }
                lanzouFile.setExtension(extension);
                if ("apk".equals(lanzouFile.getExtension())) {
                    getRealFile(lanzouFile);
                } else if (UploadService.SPLIT_FILE_EXTENSION.equals(lanzouFile.getExtension())) {
                    getRealSplitFile(lanzouFile);
                }
                if (isFolder) {
                    lanzouFile.setFolderId(Long.parseLong(id));
                } else {
                    lanzouFile.setFileId(Long.parseLong(id));
                }
                lanzouFiles.add(lanzouFile);
            }
            return lanzouFiles;
        } catch (Exception e) {
            Log.e(TAG, "getRecycleFiles:" + e);
            return Collections.emptyList();
        }
    }

    /**
     * 删除回收站文件
     *
     * @param fileId    文件 id
     * @param isFolder  是否是文件夹
     * @param isRestore 是否恢复文件
     * @return true 删除成功 false 失败
     */
    public boolean deleteRecycleFile(long fileId, boolean isFolder, boolean isRestore) {
        try {
            String html;

            String action;

            if (isRestore) {
                if (isFolder) {
                    action = "folder_restore";
                } else {
                    action = "file_restore";
                }
            } else {
                if (isFolder) {
                    action = "folder_delete_complete";
                } else {
                    action = "file_delete_complete";
                }
            }

            if (isFolder) {
                html = get(lanzouService.requestHandleRecycleFolder(action, fileId));
            } else {
                html = get(lanzouService.requestHandleRecycleFile(action, fileId));
            }

            Map<String, Object> formData = getFormData(html);

            Log.d(TAG, "params: " + formData);

            get(lanzouService.handleRecycleFile(formData));
            // FIXME: 2025/7/2 不校验是否成功，默认即成功
            return true;
        } catch (Exception e) {
            Log.e(TAG, "deleteRecycleFile:" + e.getMessage());
            return false;
        }
    }

    /**
     * 对回收站文件执行操作
     *
     * @param action 具体操作 action
     * @return 执行结果
     */
    public boolean handleRecycleFiles(String action) {
        Map<String, Object> map = new HashMap<>();
        map.put("action", action);

        try {
            String html = get(lanzouService.handleRecycleFile(map));

            if (TextUtils.isEmpty(html)) {
                return false;
            }

            Map<String, Object> formData = getFormData(html);
            get(lanzouService.handleRecycleFile(formData));
            // FIXME: 2025/7/3 不校验操作结果
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> getFormData(String html) {
        Document document = Jsoup.parse(html);
        Element form = document.selectFirst("form");
        if (form == null) {
            return Collections.emptyMap();
        }
        Elements elements = form.select("input");
        Map<String, Object> map = new HashMap<>();
        for (Element element : elements) {
            map.put(element.attr("name"), element.attr("value"));
        }
        return map;
    }

    private Request.Builder createRequest(String url) {
        int version = (int) (100 + Math.random() * 50);
        String ua = String.format(Locale.CHINA, USER_AGENT, version, version);
        Log.i(TAG, "random UserAgent: " + ua);
        // FIXME: 2025/8/1 频繁使用相同UA可能会造成无法访问问题，这里处理一下
        return new Request.Builder()
                .url(url)
                .addHeader("User-Agent", ua)
                .addHeader("Accept", ACCEPT)
                .addHeader("Accept-Language", ACCEPT_LANGUAGE);
    }

    public okhttp3.Response getResponse(String url) throws IOException {
        return okHttpClient.newCall(createRequest(url).build()).execute();
    }

    public okhttp3.Response getRangeResponse(String url, long start) throws IOException {
        Request request = createRequest(url)
                .addHeader("Range", "bytes=" + start + "-")
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
            Log.e(TAG, "get error: " + e.getMessage());
            return null;
        }
    }

}
