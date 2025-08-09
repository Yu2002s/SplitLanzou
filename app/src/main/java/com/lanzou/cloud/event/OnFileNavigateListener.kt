package com.lanzou.cloud.event

import com.lanzou.cloud.enums.FilePageType
import com.lanzou.cloud.model.FileInfoModel

interface OnFileNavigateListener {

  /**
   * 导航到
   *
   * @param fileInfoModel 点击的文件信息
   * @param position 点击的位置
   * @param filePageType 文件页面类型
   */
  fun navigate(fileInfoModel: FileInfoModel, position: Int, filePageType: FilePageType)

  /**
   * 向上导航
   *
   * @return true: finish false: 执行导航返回
   */
  fun onNavigateUp(): Boolean
}