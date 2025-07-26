package com.lanzou.cloud.ui.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.os.bundleOf
import com.drake.brv.utils.bindingAdapter
import com.drake.net.utils.scopeDialog
import com.drake.tooltip.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanzou.cloud.enums.FilePageType
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.model.FilePathModel
import com.lanzou.cloud.model.FilterSortModel
import com.lanzou.cloud.network.LanzouRepository
import com.lanzou.cloud.ui.dialog.FileDetailDialog
import com.lanzou.cloud.utils.removeModel
import com.lanzou.cloud.utils.removeModelsSuspend
import kotlinx.coroutines.delay

class LanzouFileFragment(
  position: LayoutPosition = LayoutPosition.LEFT,
  filePageType: FilePageType = FilePageType.REMOTE
) : FileFragment(position, filePageType) {

  companion object {

    private const val PARAM_FOLDER_ID = "folderId"
    private const val PARAM_NAME = "name"

    fun newInstance(
      folderId: String = "-1",
      position: LayoutPosition? = LayoutPosition.LEFT,
      name: String? = null,
    ): LanzouFileFragment {
      val fragment = LanzouFileFragment(position ?: LayoutPosition.LEFT)
      fragment.arguments = bundleOf(PARAM_FOLDER_ID to folderId, PARAM_NAME to name)
      return fragment
    }
  }

  private val clipboardManager by lazy {
    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  }

  override fun initData() {
    super.initData()
    val folderId = arguments?.getString(PARAM_FOLDER_ID) ?: "-1"
    paths.add(FilePathModel(path = folderId))
  }

  override suspend fun getData(path: String?, page: Int): List<FileInfoModel> {
    return LanzouRepository.getFiles(path ?: "-1", page)
  }

  override fun hasParentDirectory(): Boolean {
    return getCurrentPath() != "-1"
  }

  override fun onSort(
    data: List<FileInfoModel>?,
    filterSortModel: FilterSortModel
  ): List<FileInfoModel>? {
    return data
  }

  override fun isLoadMore(data: List<FileInfoModel>?): Boolean {
    return data != null && data.size >= 18
  }

  override fun onMkdir(name: String, path: String) {
    scopeDialog {
      LanzouRepository.mkdirFolder(path, name)
      super.onMkdir(name, path)
    }.catch {
      toast(it.message)
    }
  }

  override fun deleteFile(position: Int, file: FileInfoModel) {
    scopeDialog {
      LanzouRepository.deleteFile(file.fileId, file.isFile)
      mData.removeAt(binding.fileRv.removeModel(position))
    }.catch {
      toast(it.message)
    }.finally {
      toggleMulti(false)
    }
  }

  override fun deleteFiles(positions: List<Int>, files: List<FileInfoModel>) {
    scopeDialog {
      binding.fileRv.removeModelsSuspend(positions) {
        val file = models[it]
        LanzouRepository.deleteFile(file.fileId, file.isFile)
        mData.removeAt(it)
        delay(100)
      }
    }.catch {
      toast(it.message)
    }.finally {
      binding.fileRv.bindingAdapter.toggle(false)
    }
  }

  override fun renameFile(position: Int, file: FileInfoModel) {
    toast("这边重命名还没做")
  }

  override fun shareFile(position: Int, file: FileInfoModel) {
    if (file.fileId.isEmpty()) {
      toast("文件可能未上传，请刷新检查后重试")
      return
    }
    scopeDialog {
      val fileInfo = LanzouRepository.getFileInfo(file.id)
      MaterialAlertDialogBuilder(requireContext())
        .setTitle("分享")
        .setItems(arrayOf("自定义分享地址", "原始分享地址", "下载直链")) { dialog, which ->
          clipboardManager.setPrimaryClip(
            ClipData.newPlainText(
              "share", when (which) {
                0 -> fileInfo.customShareText
                1 -> fileInfo.shareText
                2 -> fileInfo.downloadUrl
                else -> fileInfo.shareText
              }
            )
          )
          toast("已复制到剪切板")
        }
        .setPositiveButton("关闭", null)
        .show()
    }.catch {
      toast(it.message)
    }
  }

  override fun showDetail(position: Int, file: FileInfoModel) {
    if (file.id.isEmpty()) {
      return
    }
    FileDetailDialog(requireContext(), file.id.toLong(), true)
      .setFileName(file.name)
  }

  override fun copyFile(
    position: Int,
    current: FileInfoModel,
    targetPath: String?
  ): FileInfoModel? {
    return null
  }

  override suspend fun moveFile(
    position: Int,
    current: FileInfoModel,
    targetPath: String?
  ): FileInfoModel? {
    targetPath ?: return null
    if (LanzouRepository.moveFile(current.id, targetPath)) {
      return current.copy()
    }
    return null
  }
}