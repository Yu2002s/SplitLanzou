package com.lanzou.cloud.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.drake.engine.base.EngineBottomSheetDialogFragment
import com.lanzou.cloud.R
import com.lanzou.cloud.databinding.DialogFileFavoriteBinding

class FileFavoriteDialog : EngineBottomSheetDialogFragment<DialogFileFavoriteBinding>() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.dialog_file_favorite, container, false)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setStyle(
      STYLE_NO_TITLE,
      com.google.android.material.R.style.Theme_Material3_DayNight_BottomSheetDialog
    )
  }

  override fun initData() {

  }

  override fun initView() {
    // setMaxWidth(percent = 1f)
  }

}