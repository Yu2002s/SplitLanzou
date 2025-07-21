package com.lanzou.cloud.ui.fragment

import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.drake.brv.utils.models
import com.drake.brv.utils.mutable
import com.drake.net.utils.scopeDialog
import com.drake.tooltip.toast
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.network.LanzouRepository

class LanzouFileFragment private constructor() : FileFragment() {

  companion object {

    private const val PARAM_FOLDER_ID = "folderId"

    fun newInstance(folderId: String = "-1"): LanzouFileFragment {
      val fragment = LanzouFileFragment()
      fragment.arguments = bundleOf(PARAM_FOLDER_ID to folderId)
      return fragment
    }
  }

  override suspend fun getData(page: Int): List<FileInfoModel> {
    val folderId = arguments?.getString(PARAM_FOLDER_ID) ?: "-1"
    return LanzouRepository.getFiles(folderId, page).toMutableList().also {
      if (folderId != "-1" && page == 1) {
        it.add(0, FileInfoModel(name = "..."))
      }
    }
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
    return false
  }

  override fun getCurrentPath(): String? {
    return arguments?.getString(PARAM_FOLDER_ID)
  }

  override fun onMkdir(name: String, path: String) {
    scopeDialog {
      LanzouRepository.mkdirFolder(path, name)
      val fileRv = binding.fileRv
      val position = if (fileRv.models.isNullOrEmpty()) 0 else 1
      binding.fileRv.mutable.add(position, FileInfoModel(name = name, folderId = path))
      super.onMkdir(name, path)
    }.catch {
      toast(it.message)
    }
  }
}