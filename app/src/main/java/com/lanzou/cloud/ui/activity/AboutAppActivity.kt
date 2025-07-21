package com.lanzou.cloud.ui.activity

import android.view.View
import com.drake.engine.utils.AppUtils
import com.lanzou.cloud.LanzouApplication
import com.lanzou.cloud.R
import com.lanzou.cloud.base.BaseToolbarActivity
import com.lanzou.cloud.databinding.ActivityAboutAppBinding
import com.lanzou.cloud.utils.UpdateUtils

class AboutAppActivity : BaseToolbarActivity<ActivityAboutAppBinding>(R.layout.activity_about_app) {
  override fun initData() {

  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.tv_gitee -> WebActivity.actionStart(LanzouApplication.GITEE_HOME)
      R.id.tv_github -> WebActivity.actionStart(LanzouApplication.GITHUB_HOME)
      R.id.tv_donate -> WebActivity.actionStart(LanzouApplication.APP_DONATE)
      R.id.check_update -> UpdateUtils.checkUpdate(this)
    }
  }

  override fun initView() {
    binding.tvVersion.text = "v${AppUtils.getAppVersionName()}(${AppUtils.getAppVersionCode()})"
  }
}