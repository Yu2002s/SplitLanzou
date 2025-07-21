package com.lanzou.cloud.model

import android.os.Environment

data class FilePathModel(
  val path: String = Environment.getExternalStorageDirectory().path,
  var scrollPosition: Int = 0,
)