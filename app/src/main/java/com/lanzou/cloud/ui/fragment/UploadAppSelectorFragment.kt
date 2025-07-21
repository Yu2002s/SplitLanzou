package com.lanzou.cloud.ui.fragment

import android.content.pm.PackageManager
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.utils.formatBytes
import java.io.File

class UploadAppSelectorFragment : FileFragment(LayoutPosition.RIGHT) {

  override suspend fun getData(page: Int): List<FileInfoModel>? {
    val pm = requireContext().packageManager
    return requireContext().packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)
      .mapNotNull {
        val applicationInfo = it.applicationInfo ?: return null
        val name = applicationInfo.loadLabel(pm).toString()
        val length = File(applicationInfo.sourceDir).length()
        val size = length.formatBytes()
        val versionName = it.versionName ?: ""
        FileInfoModel(
          name = name,
          extension = "apk",
          size = size,
          length = length,
          updateTime = it.lastUpdateTime,
          pkgName = it.packageName,
          versionName = versionName,
          path = File(applicationInfo.sourceDir).path
        )
      }.sorted()
  }

  override fun onBack(): Boolean {
    return true
  }

  override fun onLayoutChange(positon: LayoutPosition) {
    super.onLayoutChange(positon)
  }

}