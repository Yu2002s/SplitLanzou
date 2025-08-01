package com.lanzou.cloud.event;

public interface Searchable {

    /**
     * 执行搜索事件
     *
     * @param keyWorld 关键字
     */
    void onSearch(String keyWorld);
}
