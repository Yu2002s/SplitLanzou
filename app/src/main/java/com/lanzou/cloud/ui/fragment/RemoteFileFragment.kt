package com.lanzou.cloud.ui.fragment

import com.lanzou.cloud.enums.FilePageType
import com.lanzou.cloud.enums.LayoutPosition

/**
 * 远程文件管理
 */
abstract class RemoteFileFragment(
  position: LayoutPosition = LayoutPosition.LEFT,
  filePageType: FilePageType = FilePageType.REMOTE
) : FileFragment(position, filePageType) {

  override fun onMkdirFile(name: String, path: String) {
    super.onMkdirFile(name, path)
  }

}