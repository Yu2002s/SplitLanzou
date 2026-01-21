package com.lanzou.cloud.model

import androidx.databinding.BaseObservable

data class LanzouResolveFileModel(
  var url: String = "",
  var pwd: String? = null,
  var downloadUrl: String = "",
  var fileName: String = "",
  var fileSize: String = "",
  var shareTime: String = "",
  var remark: String = "",
  var isFile: Boolean = true
) : BaseObservable()