package com.lanzou.cloud.ui.fragment

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.drake.engine.utils.AppUtils
import com.drake.engine.utils.FileUtils
import com.drake.net.utils.scopeDialog
import com.drake.tooltip.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanzou.cloud.LanzouApplication
import com.lanzou.cloud.base.BaseEditDialog
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.utils.formatBytes
import com.lanzou.cloud.utils.updateModel
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class UploadAppSelectorFragment(position: LayoutPosition = LayoutPosition.RIGHT) :
  FileFragment(position) {

  override suspend fun getData(page: Int): List<FileInfoModel>? {
    val pm = requireContext().packageManager
    var packageInfoList =
      requireContext().packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)
    if (!viewModel.filterSortModel.value.showSystemApp) {
      packageInfoList =
        packageInfoList.filter { (it.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
    }
    return packageInfoList.mapNotNull {
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
    }
  }

  override fun onNavigateUp(): Boolean {
    return true
  }

  override fun deleteFiles(positions: List<Int>, files: List<FileInfoModel>) {
    toast("不支持多选删除App")
  }

  override fun deleteFile(position: Int, file: FileInfoModel) {
    if (file.pkgName.isNullOrEmpty()) {
      return
    }
    AppUtils.uninstallApp(file.pkgName)
  }

  override fun renameFile(position: Int, file: FileInfoModel) {
    BaseEditDialog(requireContext()).apply {
      setTitle("重命名App")
      editValue = file.name + ".apk"
      hint = "新文件名(注意定时清理App缓存)"
      setPositiveButton("修改") { dialog, _ ->
        if (editValue.isEmpty()) {
          toast("请输入文件名称")
          return@setPositiveButton
        }
        file.name = editValue
        val targetFile = File(file.path)
        val renamedFile = File(LanzouApplication.tempPath, editValue)
        if (FileUtils.copyFile(targetFile, renamedFile) { true }) {
          file.path = renamedFile.path
        }
        binding.fileRv.updateModel(position)
      }
      setNegativeButton("取消", null)
      show()
    }
  }

  override fun showDetail(position: Int, file: FileInfoModel) {
    val appInfo = AppUtils.getAppInfo(file.pkgName)
    val packageInfo = requireContext().packageManager.getPackageInfo(file.pkgName!!, 0)

    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
    val installTime = simpleDateFormat.format(packageInfo.firstInstallTime)
    val updateTime = simpleDateFormat.format(packageInfo.lastUpdateTime)

    val items = arrayOf(
      "包名: ${appInfo.packageName}",
      "版本名: ${appInfo.versionName}",
      "版本号: ${appInfo.versionCode}",
      "安装目录: ${appInfo.packagePath}",
      "类型: ${if (appInfo.isSystem) "系统App" else "用户App"}",
      "大小: ${file.size}",
      "安装时间: $installTime",
      "更新时间: $updateTime",
    )
    MaterialAlertDialogBuilder(requireContext())
      .setIcon(appInfo.icon)
      .setTitle(appInfo.name)
      .setItems(items, null)
      .setPositiveButton("关闭", null)
      .setNegativeButton("打开") { dialog, _ ->
        AppUtils.launchApp(appInfo.packageName)
      }
      .setNeutralButton("设置") { dialog, _ ->
        AppUtils.launchAppDetailsSettings(appInfo.packageName)
      }
      .show()
  }

  override suspend fun moveFile(
    position: Int,
    current: FileInfoModel,
    targetPath: String?
  ): FileInfoModel? {
    return null
  }

  override fun shareFile(position: Int, file: FileInfoModel) {
    file.pkgName ?: return
    scopeDialog(dispatcher = Dispatchers.IO) {
      com.lanzou.cloud.utils.FileUtils.shareApp(requireContext(), file.pkgName)
    }
  }
}