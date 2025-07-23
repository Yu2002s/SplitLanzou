package com.lanzou.cloud.ui.fragment

import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.mutable
import com.drake.net.utils.scopeDialog
import com.drake.tooltip.toast
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.event.OnFileNavigateListener
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.model.FilterSortModel
import com.lanzou.cloud.network.LanzouRepository
import com.lanzou.cloud.ui.dialog.FileDetailDialog
import com.lanzou.cloud.utils.removeModel
import com.lanzou.cloud.utils.removeModelsSuspend
import kotlinx.coroutines.delay

class LanzouFileFragment private constructor() : FileFragment() {

  companion object {

    private const val PARAM_FOLDER_ID = "folderId"

    fun newInstance(folderId: String = "-1"): LanzouFileFragment {
      val fragment = LanzouFileFragment()
      fragment.arguments = bundleOf(PARAM_FOLDER_ID to folderId)
      return fragment
    }
  }

  private var folderId = "-1"

  override fun initData() {
    super.initData()
    folderId = arguments?.getString(PARAM_FOLDER_ID) ?: "-1"
  }

  override suspend fun getData(page: Int): List<FileInfoModel> {
    return LanzouRepository.getFiles(folderId, page)
  }

  override fun showBackItem(): Boolean {
    return folderId != "-1"
  }

  override fun onSort(
    data: List<FileInfoModel>?,
    filterSortModel: FilterSortModel
  ): List<FileInfoModel>? {
    return data
  }

  override fun onLoadEnd(data: List<FileInfoModel>?) {
  }

  override fun isLoadMore(data: List<FileInfoModel>?): Boolean {
    return data != null && data.size >= 18
  }

  override fun onLayoutChange(positon: LayoutPosition) {
    binding.fileRv.layoutManager = when (positon) {
      LayoutPosition.LEFT, LayoutPosition.MIDDLE -> LinearLayoutManager(requireContext())
      LayoutPosition.RIGHT -> StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
    }
  }

  override fun onBack(): Boolean {
    val parentFragment = parentFragment ?: return false
    if (parentFragment is OnFileNavigateListener) {
      return parentFragment.onNavigateUp()
    }
    return false
  }

  override fun getCurrentPath(): String? {
    return arguments?.getString(PARAM_FOLDER_ID)
  }

  override fun onMkdir(name: String, path: String) {
    scopeDialog {
      LanzouRepository.mkdirFolder(path, name)
      val position = getInsertPosition()
      binding.fileRv.mutable.add(position, FileInfoModel(name = name, folderId = path))
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

  override fun showDetail(position: Int, file: FileInfoModel) {
    if (file.id.isEmpty()) {
      return
    }
    FileDetailDialog(requireContext(), file.id.toLong(), true)
      .setFileName(file.name)
  }
}