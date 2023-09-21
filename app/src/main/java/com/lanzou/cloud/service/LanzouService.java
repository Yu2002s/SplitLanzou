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
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface LanzouService {

    /**
     * task 47加载文件夹，5 加载文件
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
     * 上传文件 1
     */
    @POST("html5up.php")
    Call<LanzouUploadResponse> uploadFile(@Body RequestBody requestBody);

    /**
     * task 19 获取所有文件夹
     */
    @POST("doupload.php")
    @FormUrlEncoded
    Call<LanzouFolderResponse> getAllFolder(@Field("task") int task);

    /**
     * task 22 获取文件的分享链
     */
    @POST("doupload.php")
    @FormUrlEncoded
    Call<LanzouUrlResponse> getShareUrl(
            @Field("task") int task,
            @Field("file_id") long fileId
    );

    @POST
    Call<LanzouDownloadResponse> getDownloadUrl(
            @Header("User-Agent") String userAgent,
            @Header("Referer") String referer,
            @Url String url,
            @Body FormBody body);

    /**
     * task 2 新建文件夹
     */
    @POST("doupload.php")
    @FormUrlEncoded
    Call<LanzouSimpleResponse> createFolder(
            @Field("task") int task,
            @Field("parent_id") long parentId,
            @Field("folder_name") String name,
            @Field("folder_description") String desc
    );

    @POST("doupload.php")
    @FormUrlEncoded
    Call<LanzouSimpleResponse> deleteFile(
            @FieldMap Map<String, String> map
    );

    @POST("doupload.php")
    @FormUrlEncoded
    Call<LanzouSimpleResponse> moveFile(
            @Field("task") int task,
            @Field("file_id") long fileId,
            @Field("folder_id") long folderId
    );
}
