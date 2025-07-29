package com.lanzou.cloud.model

import android.os.Environment

data class FilePathModel(
  /**
   * 可以是路径，可以是文件夹 id
   */
  val path: String = Environment.getExternalStorageDirectory().path,
  val name: String = "",
  var scrollPosition: Int = 0,
)