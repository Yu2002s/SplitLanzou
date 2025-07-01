package com.lanzou.cloud.ui.file;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.cloud.data.LanzouFile;
import com.lanzou.cloud.data.LanzouPage;
import com.lanzou.cloud.event.FileActionListener;

import java.util.List;

/**
 * 文件操作相关的接口
 */
public interface FileAction {

    /**
     * 执行刷新操作，刷新文件列表
     */
    void refresh();

    /**
     * 通过文件夹 id 和 文件夹名称获取文件列表
     *
     * @param folderId   文件夹 id
     * @param folderName 文件夹名称
     */
    void getFiles(long folderId, String folderName);

    /**
     * 返回事件回调
     */
    void onBackPressed();

    /**
     * 加载更多文件
     */
    void loadMoreFiles();

    /**
     * 获取到源文件（过滤之前的文件）
     *
     * @return 文件列表
     */
    List<LanzouFile> getSource();

    /**
     * 获取所有文件页面
     *
     * @return 页面集合
     */
    List<LanzouPage> getLanzouPages();

    /**
     * 获取当前的文件页面
     *
     * @return 当前文件页面
     */
    LanzouPage getCurrentPage();

    /**
     * 绑定到指定的 RecyclerView
     *
     * @param rv                 RecyclerView
     * @param fileActionListener 文件操作监听回调
     * @see FileActionListener
     */
    void bindView(RecyclerView rv, FileActionListener fileActionListener);

    /**
     * 获取根目录的文件列表
     */
    default void getFiles() {
        getFiles(-1, "根目录");
    }

    /**
     * 导航到指定的页面
     * @param position 页面的位于 <code>lanzouPages</code> 中的位置（0开始）
     */
    boolean navigateTo(int position);

    /**
     * 删除指定的文件项
     * @param position 文件在文件列表中的位置
     */
    void deleteItem(int position);

    /**
     * 新建文件夹
     */
    void createFolder();

    /**
     * 删除指定的文件（本地+服务端）
     * @param position 文件在文件列表中的位置
     */
    void deleteFile(int position);

    /**
     * 删除多个文件
     * @param callback 操作回调
     * @see Callback
     */
    void deleteFiles(Callback callback);

    /**
     * 分享文件
     * @param position 文件在文件列表中的位置
     */
    void shareFile(int position);

    /**
     * 移动文件
     * @param lanzouFile 指定要移动的文件
     */
    void moveFile(LanzouFile lanzouFile);

    /**
     * 移动多个文件
     */
    void moveFiles();

    /**
     * 订阅
     *
     * @param context fragment 实例
     */
    void observable(Fragment context);

    /**
     * 操作回调
     */
    interface Callback {
        /**
         * 操作完成
         */
        void onCompleted();
    }
}
