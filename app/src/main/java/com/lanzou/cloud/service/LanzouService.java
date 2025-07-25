package com.lanzou.cloud.service;

import com.lanzou.cloud.data.LanzouDownloadResponse;
import com.lanzou.cloud.data.LanzouFileResponse;
import com.lanzou.cloud.data.LanzouFolderResponse;
import com.lanzou.cloud.data.LanzouSimpleResponse;
import com.lanzou.cloud.data.LanzouUploadResponse;
import com.lanzou.cloud.data.LanzouUrlResponse;

import java.util.Map;

import okhttp3.FormBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;

/**
 * API 接口
 */
public interface LanzouService {

    /**
     * 获取文件列表
     *
     * @param task     47加载文件夹，5 加载文件
     * @param uid      用户 UID
     * @param folderId 父文件夹 id
     * @param page     页码
     */
    @POST("doupload.php")
    @FormUrlEncoded
    Call<LanzouFileResponse> getFiles(
            @Query("uid") long uid,
            @Field("task") int task,
            @Field("folder_id") long folderId,
            @Field("pg") int page
    );

    /**
     * 上传文件 task: 1
     *
     * @param requestBody 请求体数据
     */
    @POST("html5up.php")
    Call<LanzouUploadResponse> uploadFile(@Body RequestBody requestBody);

    /**
     * 获取所有文件夹
     *
     * @param task 19
     */
    @POST("doupload.php")
    @FormUrlEncoded
    Call<LanzouFolderResponse> getAllFolder(@Field("task") int task);

    /**
     * 获取文件的分享链
     *
     * @param task   22
     * @param fileId 文件 id
     */
    @POST("doupload.php")
    @FormUrlEncoded
    Call<LanzouUrlResponse> getShareUrl(
            @Field("task") int task,
            @Field("file_id") long fileId
    );

    /**
     * 获取下载地址，先改用接口进行获取
     *
     * @param userAgent 代理
     * @param referer   referer
     * @param url       分享地址
     * @param body      表单数据
     */
    @Deprecated
    @POST
    Call<LanzouDownloadResponse> getDownloadUrl(
            @Header("User-Agent") String userAgent,
            @Header("Referer") String referer,
            @Url String url,
            @Body FormBody body);

    /**
     * 创建文件夹
     *
     * @param task     2 新建文件夹
     * @param parentId 父文件夹 id
     * @param name     文件夹名称
     * @param desc     文件夹描述消息
     */
    @POST("doupload.php")
    @FormUrlEncoded
    Call<LanzouSimpleResponse> createFolder(
            @Field("task") int task,
            @Field("parent_id") long parentId,
            @Field("folder_name") String name,
            @Field("folder_description") String desc
    );

    /**
     * 删除文件
     *
     * @param map 所有参数
     * @return 操作结果
     */
    @POST("doupload.php")
    @FormUrlEncoded
    Call<LanzouSimpleResponse> deleteFile(
            @FieldMap Map<String, String> map
    );

    /**
     * 移动文件
     *
     * @param task     20
     * @param fileId   文件 id
     * @param folderId 目标文件夹 id
     */
    @POST("doupload.php")
    @FormUrlEncoded
    Call<LanzouSimpleResponse> moveFile(
            @Field("task") int task,
            @Field("file_id") long fileId,
            @Field("folder_id") long folderId
    );

    /**
     * task 18 得到文件夹信息
     *
     * @param folderId 文件夹 id
     * @param task     18
     */
    @POST("doupload.php")
    @FormUrlEncoded
    Call<LanzouUrlResponse> getFolder(
            @Field("task") int task,
            @Field("folder_id") long folderId
    );

    /**
     * 修改文件夹密码
     *
     * @param task      23 修改文件密码，16 修改文件夹密码
     * @param fileId    文件 id
     * @param enablePwd 是否开启密码，1开启，0不开启
     * @param password  文件密码
     */
    @POST("doupload.php")
    @FormUrlEncoded
    Call<LanzouSimpleResponse> editFolderPassword(
            @Field("task") int task,
            @Field("folder_id") long fileId,
            @Field("shows") int enablePwd,
            @Field("shownames") String password
    );

    /**
     * 修改文件密码
     *
     * @param task      23 修改文件密码，16 修改文件夹密码
     * @param fileId    文件 id
     * @param enablePwd 是否开启密码，1开启，0不开启
     * @param password  文件密码
     */
    @POST("doupload.php")
    @FormUrlEncoded
    Call<LanzouSimpleResponse> editFilePassword(
            @Field("task") int task,
            @Field("file_id") long fileId,
            @Field("shows") int enablePwd,
            @Field("shownames") String password
    );

    /**
     * 获取回收站文件列表
     *
     * @return 网页内容
     */
    @GET("/mydisk.php?item=recycle&action=files")
    Call<String> getRecycleFiles();

    /**
     * 请求删除文件夹
     *
     * @param folderId 文件夹 id
     * @return 删除结果网页内容
     */
    @GET("/mydisk.php?item=recycle")
    Call<String> requestHandleRecycleFolder(
            @Query("action") String action,
            @Query("folder_id") long folderId
    );

    /**
     * 请求删除文件
     *
     * @param fileId 文件 id
     * @return 网页内容
     */
    @GET("/mydisk.php?item=recycle")
    Call<String> requestHandleRecycleFile(
            @Query("action") String action,
            @Query("file_id") long fileId
    );

    /**
     * 处理回收站文件
     *
     * @param map 所需参数
     * @return 结果
     */
    @POST("mydisk.php?item=recycle")
    @FormUrlEncoded
    Call<String> handleRecycleFile(@FieldMap Map<String, Object> map);
}
