package com.lanzou.cloud.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.drake.engine.base.EngineBottomSheetDialogFragment
import com.drake.tooltip.toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lanzou.cloud.R
import com.lanzou.cloud.databinding.DialogFileResolveBinding
import com.lanzou.cloud.service.DownloadService
import kotlinx.coroutines.launch

class FileResolveDialog(private val downloadService: DownloadService? = null) :
  EngineBottomSheetDialogFragment<DialogFileResolveBinding>() {

  private val fileResolveViewModel by activityViewModels<FileResolveViewModel>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.dialog_file_resolve, container, false)
  }

  override fun initData() {
    binding.lifecycleOwner = this
    behavior.state = BottomSheetBehavior.STATE_EXPANDED

    lifecycleScope.launch {
      fileResolveViewModel.lanzouResolveFile.collect {
        binding.m = it
      }
    }

    binding.btnResolve.setOnClickListener {
      fileResolveViewModel.updatePwd()
    }

    binding.btnClose.setOnClickListener {
      dismiss()
    }

    binding.btnFavorite.setOnClickListener {
      // TODO: 收藏文件
    }

    binding.btnDownload.setOnClickListener {
      val file = fileResolveViewModel.lanzouResolveFile.value
      if (file == null) {
        toast("请先解析文件完成")
        return@setOnClickListener
      }
      if (file.url.isEmpty()) {
        toast("文件分享链接不能为空")
        return@setOnClickListener
      }
      downloadService?.addDownload(file.url, file.fileName, file.pwd)
    }
  }

  override fun initView() {

  }


}