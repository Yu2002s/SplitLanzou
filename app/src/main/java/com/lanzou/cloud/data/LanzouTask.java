package com.lanzou.cloud.data;

/**
 * 对应 Lanzou 的各种文件操作的 task
 */
public enum LanzouTask {

    /**
     * 获取文件列表
     */
    GET_FILES(5),
    /**
     * 获取文件夹列表
     */
    GET_FOLDERS(47);

    public final Integer id;

    LanzouTask(Integer id) {
        this.id = id;
    }
}
