package com.lanzou.cloud.ui.fragment

import com.drake.engine.utils.FileUtils
import com.drake.engine.utils.PinyinUtils
import com.drake.net.utils.scopeDialog
import com.drake.net.utils.withIO
import com.drake.tooltip.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanzou.cloud.enums.FileSortField
import com.lanzou.cloud.enums.FileSortRule
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.model.FilterSortModel
import com.lanzou.cloud.utils.FileJavaUtils
import com.lanzou.cloud.utils.removeModels
import kotlinx.coroutines.coroutineScope
import java.io.File

/**
 * 管理本地文件
 */
abstract class LocalFileFragment(position: LayoutPosition = LayoutPosition.RIGHT) :
  FileFragment(position) {

  override fun onSort(
    data: List<FileInfoModel>?,
    filterSortModel: FilterSortModel
  ): List<FileInfoModel>? {
    val rule = filterSortModel.rule
    val field = filterSortModel.field

    return data?.sortedWith { o1, o2 ->
      if (o1.isDirectory && o2.isFile) {
        -1
      } else if (o1.isFile && o2.isDirectory) {
        1
      } else {
        when (rule) {
          FileSortRule.ASC -> when (field) {
            FileSortField.NAME -> PinyinUtils.ccs2Pinyin(o1.name)
              .compareTo(PinyinUtils.ccs2Pinyin(o2.name))

            FileSortField.TIME -> o1.updateTime.compareTo(o2.updateTime)
            FileSortField.SIZE -> o1.length.compareTo(o2.length)
          }

          FileSortRule.DESC -> when (field) {
            FileSortField.NAME ->
              PinyinUtils.ccs2Pinyin(o2.name)
                .compareTo(PinyinUtils.ccs2Pinyin(o1.name))

            FileSortField.TIME -> o2.updateTime.compareTo(o1.updateTime)
            FileSortField.SIZE -> o2.length.compareTo(o1.length)
          }
        }
      }
    }
  }

  override fun deleteFile(position: Int, file: FileInfoModel) {
    if (file.path.isEmpty()) {
      return
    }
    MaterialAlertDialogBuilder(requireContext())
      .setTitle("删除文件")
      .setMessage("确认要删除文件: ${file.name}")
      .setPositiveButton("确认") { dialog, _ ->
        scopeDialog {
          withIO {
            File(file.path).deleteRecursively()
          }
          removeFile(position, file)
        }.finally {
          toggleMulti(false)
        }
      }
      .setNegativeButton("取消", null)
      .show()
  }

  override fun deleteFiles(positions: List<Int>, files: List<FileInfoModel>) {
    scopeDialog {
      withIO {
        files.forEach {
          if (it.path.isNotEmpty()) {
            File(it.path).deleteRecursively()
          }
        }
      }
      binding.fileRv.removeModels(positions) {
        sourceData.removeAt(it)
      }
    }.finally {
      toggleMulti(false)
    }
  }

  override suspend fun onRenameFile(file: FileInfoModel): Boolean {
    val targetFile = File(file.path)
    val renamedFile = File(targetFile.parentFile, file.name)
    if (targetFile.renameTo(renamedFile)) {
      file.path = renamedFile.path
      return true
    }
    return false
  }

  override fun showFileDetail(position: Int, file: FileInfoModel) {
    val items = arrayOf(
      "文件名称: ${file.name}",
      "目录: ${file.path}",
      "类型: ${file.extension}",
      "大小: ${file.size}",
      "修改时间: ${file.updateTimeStr}"
    )
    MaterialAlertDialogBuilder(requireContext())
      .setTitle("文件详情")
      .setItems(items, null)
      .setPositiveButton("关闭", null)
      .setNegativeButton("打开文件") { dialog, _ ->
        FileJavaUtils.openFile(file.path)
      }
      .setNeutralButton("MD5") { dialog, _ ->
        scopeDialog {
          val md5 = coroutineScope {
            FileUtils.getFileMD5ToString(file.path)
          }
          MaterialAlertDialogBuilder(requireContext())
            .setTitle("文件MD5")
            .setMessage(md5)
            .setPositiveButton("关闭", null)
            .show()
        }
      }
      .show()
  }

  override suspend fun moveFile(
    position: Int,
    current: FileInfoModel,
    targetPath: String?
  ): FileInfoModel? {
    targetPath ?: return null
    // FIXME: 不判断存不存在，暂时直接替换文件
    val targetFilePath = targetPath + File.separator + current.name
    if (current.path == targetFilePath) {
      return null
    }
    val result = if (current.isDirectory) {
      FileUtils.moveDir(current.path, targetFilePath) { true }
    } else {
      FileUtils.moveFile(current.path, targetFilePath) { true }
    }

    if (result) {
      return current.copy(path = targetFilePath)
    }
    return null
  }

  override fun shareFile(position: Int, file: FileInfoModel) {
    if (!FileUtils.isFile(file.path)) {
      toast("文件可能未下载完成，请刷新检查后重试")
      return
    }
    com.lanzou.cloud.utils.FileUtils.shareFile(requireContext(), file.path)
  }

  override suspend fun copyFile(
    position: Int,
    current: FileInfoModel,
    targetPath: String?
  ): FileInfoModel? {
    targetPath ?: return null
    val targetFilePath = targetPath + File.separator + current.name
    if (current.path == targetFilePath) {
      return null
    }
    try {
      val file = File(current.path)
      val target = File(targetFilePath)
      file.copyRecursively(target, true)
      return current.copy(path = targetFilePath)
    } catch (_: Exception) {
      return null
    }
  }
}