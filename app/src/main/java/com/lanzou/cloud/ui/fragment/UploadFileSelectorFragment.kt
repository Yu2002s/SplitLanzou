package com.lanzou.cloud.ui.fragment

import android.os.Environment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.drake.brv.utils.models
import com.drake.brv.utils.mutable
import com.drake.engine.utils.FileUtils
import com.drake.tooltip.toast
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.model.FilePathModel
import com.lanzou.cloud.utils.formatBytes
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class UploadFileSelectorFragment : FileFragment(LayoutPosition.RIGHT) {

  companion object {
    private val ROOT = Environment.getExternalStorageDirectory()
  }

  private val pathList = mutableListOf(FilePathModel(ROOT.path))

  private val currentPath get() = pathList.last()

  override suspend fun getData(page: Int): List<FileInfoModel>? {
    return getFiles(currentPath.path)
  }

  private fun getFiles(path: String): List<FileInfoModel>? {
    val simpleDataFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    return FileUtils.listFilesInDir(path)?.map {
      val time = simpleDataFormat.format(it.lastModified()).substring(2)
      val ext = if (it.isFile) FileUtils.getFileExtension(it.path) else null
      FileInfoModel(
        name = it.name,
        length = it.length(),
        size = it.length().formatBytes(),
        updateTimeStr = time,
        updateTime = it.lastModified(),
        extension = ext,
        path = it.path
      )
    }
  }

  override fun showBackItem(): Boolean {
    return currentPath.path != ROOT.path
  }

  override fun onLoadEnd(data: List<FileInfoModel>?) {
    val scrollPosition = pathList.last().scrollPosition
    scrollToPosition(scrollPosition)
  }

  override fun onItemClick(model: FileInfoModel, position: Int) {
    if (model.isFile) {
      // 请求上传文件
      super.onItemClick(model, position)
      return
    }
    val currentScrollPosition = getFirstVisiblePosition()
    pathList[pathList.lastIndex].scrollPosition = currentScrollPosition
    pathList.add(FilePathModel(model.path))
    binding.refresh.showLoading()
  }

  private fun getFirstVisiblePosition(): Int {
    val layoutManager = binding.fileRv.layoutManager
    return when (layoutManager) {
      is LinearLayoutManager -> layoutManager.findFirstCompletelyVisibleItemPosition()
      is StaggeredGridLayoutManager -> {
        val arr = layoutManager.findFirstCompletelyVisibleItemPositions(null)
        arr[0]
      }

      else -> 0
    }
  }

  // FIXME: 暂时使用父类的
  override fun getInsertPosition(): Int {
    return super.getInsertPosition()
  }

  override fun onBack(): Boolean {
    if (pathList.size == 1) {
      // 根目录默认返回
      return super.onBack()
    }
    pathList.removeAt(pathList.lastIndex)
    binding.refresh.showLoading()
    return false
  }

  override fun getCurrentPath(): String? {
    return currentPath.path
  }

  override fun onMkdir(name: String, path: String) {
    val file = File(path)
    if (file.exists()) {
      toast("文件已存在")
      return
    }
    if (file.mkdir()) {
      val fileRv = binding.fileRv
      val position = if (fileRv.models.isNullOrEmpty()) 0 else 1
      binding.fileRv.mutable.add(position, FileInfoModel(name = name, path = path))
      super.onMkdir(name, path)
    } else {
      toast("创建失败，请检查App储存权限")
    }
  }
}