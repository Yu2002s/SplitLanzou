package com.lanzou.cloud.ui.fragment

import com.drake.net.utils.scopeDialog
import com.lanzou.cloud.enums.FilePageType
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.model.LanzouResolveFileModel
import com.lanzou.cloud.network.LanzouRepository
import com.lanzou.cloud.ui.dialog.FileFavoriteDialog

/**
 * 远程文件管理
 */
abstract class RemoteFileFragment(
  position: LayoutPosition = LayoutPosition.LEFT,
  filePageType: FilePageType = FilePageType.REMOTE
) : FileFragment(position, filePageType) {

  override fun favoriteFile(file: FileInfoModel) {
    scopeDialog {
      val fileInfo = LanzouRepository.getFileInfo(file.id)
      FileFavoriteDialog(
        LanzouResolveFileModel(
          url = fileInfo.shareUrl,
          pwd = if (fileInfo.hasPwd == 1) fileInfo.pwd else null,
          downloadUrl = fileInfo.downloadUrl,
          fileName = file.name,
          fileSize = file.size,
          shareTime = file.updateTimeStr,
          remark = "",
        )
      ).show(childFragmentManager, "FileFavoriteDialog")
    }
  }
}