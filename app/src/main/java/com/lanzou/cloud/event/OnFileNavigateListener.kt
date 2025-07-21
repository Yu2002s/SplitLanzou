package com.lanzou.cloud.event

import com.lanzou.cloud.model.FileInfoModel

interface OnFileNavigateListener {

  fun navigate(fileInfoModel: FileInfoModel, position: Int)
}