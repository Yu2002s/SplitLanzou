package com.lanzou.cloud.ui.fragment

import android.os.Environment
import androidx.core.os.bundleOf
import com.drake.engine.utils.FileUtils
import com.drake.tooltip.toast
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.model.FilePathModel
import com.lanzou.cloud.utils.formatBytes
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 手机文件管理
 */
class PhoneFileFragment(position: LayoutPosition = LayoutPosition.RIGHT) :
  LocalFileFragment(position) {

  companion object {
    private val ROOT = Environment.getExternalStorageDirectory()

    private const val PARAM_PATH = "path"

    fun newInstance(
      path: String? = ROOT.path,
      position: LayoutPosition? = LayoutPosition.RIGHT
    ): PhoneFileFragment {
      val fragment = PhoneFileFragment(position ?: LayoutPosition.RIGHT)
      fragment.arguments = bundleOf(PARAM_PATH to (path ?: ROOT.path))
      return fragment
    }
  }

  override fun initData() {
    super.initData()
    paths.add(FilePathModel(name = "根目录", path = arguments?.getString(PARAM_PATH) ?: ROOT.path))
  }

  override suspend fun getData(path: String?, page: Int): List<FileInfoModel>? {
    return getFiles(path ?: ROOT.path)
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

  override fun hasParentDirectory(): Boolean {
    return getCurrentPath() != ROOT.path
  }

  override fun getInsertPosition(name: String?): Int {
    // TODO: 有问题，暂时用父类的
    return super.getInsertPosition(name)
    /*if (name.isNullOrEmpty() || models.isEmpty()) {
      return super.getInsertPosition(name)
    }
    val namePinyin = PinyinUtils.ccs2Pinyin(name)
    // 查找第一个拼音大于name的文件（正确比较逻辑）
    val index = models.indexOfFirst {
      it.isFile && PinyinUtils.ccs2Pinyin(it.name) > namePinyin
    }
    return if (index != -1) index else super.getInsertPosition(name)*/
  }

  override fun onMkdirFile(name: String, path: String) {
    val file = File(path)
    if (file.exists()) {
      toast("文件已存在")
      return
    }
    if (file.mkdir()) {
      super.onMkdirFile(name, path)
    } else {
      toast("创建失败，请检查App储存权限")
    }
  }

  override fun onItemLongClick(model: FileInfoModel, position: Int): Boolean {
    if (model.isDirectory) {
      // 文件夹重命名
      renameFile(position, model)
      return true
    }
    return false
  }
}