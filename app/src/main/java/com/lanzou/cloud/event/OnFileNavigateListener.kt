package com.lanzou.cloud.event

import com.lanzou.cloud.enums.FilePageType
import com.lanzou.cloud.model.FileInfoModel

interface OnFileNavigateListener {

  fun navigate(fileInfoModel: FileInfoModel, position: Int, filePageType: FilePageType)

  fun onNavigateUp(): Boolean
}