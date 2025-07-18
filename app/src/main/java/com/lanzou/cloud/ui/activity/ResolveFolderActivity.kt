package com.lanzou.cloud.ui.activity

import com.lanzou.cloud.R
import com.lanzou.cloud.base.BaseToolbarActivity
import com.lanzou.cloud.databinding.ActivityResolveFolderBinding

/**
 * 解析分享文件夹
 */
class ResolveFolderActivity :
  BaseToolbarActivity<ActivityResolveFolderBinding>(R.layout.activity_resolve_folder) {
  override fun initData() {

  }

  override fun initView() {
    setTitle(getString(R.string.title_resolve_folder))
  }
}