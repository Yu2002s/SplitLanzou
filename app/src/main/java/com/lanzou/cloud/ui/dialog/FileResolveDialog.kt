package com.lanzou.cloud.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.drake.engine.base.EngineBottomSheetDialogFragment
import com.lanzou.cloud.R
import com.lanzou.cloud.databinding.DialogFileResolveBinding
import kotlinx.coroutines.launch

class FileResolveDialog : EngineBottomSheetDialogFragment<DialogFileResolveBinding>() {

  private val fileResolveViewModel by activityViewModels<FileResolveViewModel>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.dialog_file_resolve, container, false)
  }

  override fun initData() {
    binding.m = fileResolveViewModel
    binding.lifecycleOwner = this
    lifecycleScope.launch {
      fileResolveViewModel.fileShareUrl.collect {

      }
    }
  }

  override fun initView() {

  }


}