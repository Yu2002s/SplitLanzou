package com.lanzou.cloud.model

import android.os.Environment

/**
 * 文件路径对象
 */
data class FilePathModel(
  /**
   * 可以是路径，可以是文件夹 id
   */
  val path: String = Environment.getExternalStorageDirectory().path,
  /**
   * 路径名称
   */
  val name: String = "",
  /**
   * 当前滚动的位置
   */
  var scrollPosition: Int = 0,
)