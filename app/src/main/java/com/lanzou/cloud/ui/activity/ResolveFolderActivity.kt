package com.lanzou.cloud.ui.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.drake.brv.annotaion.AnimationType
import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.divider
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.net.utils.scope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanzou.cloud.LanzouApplication
import com.lanzou.cloud.R
import com.lanzou.cloud.base.BaseToolbarActivity
import com.lanzou.cloud.databinding.ActivityResolveFolderBinding
import com.lanzou.cloud.model.LanzouShareFolderModel
import com.lanzou.cloud.network.LanzouRepository
import com.lanzou.cloud.service.DownloadService
import com.lanzou.cloud.utils.fitNavigationBar
import com.lanzou.cloud.utils.startActivity

/**
 * 解析分享文件夹
 */
class ResolveFolderActivity :
  BaseToolbarActivity<ActivityResolveFolderBinding>(R.layout.activity_resolve_folder),
  ServiceConnection {

  companion object {

    private const val PARAM_URL = "url"

    private const val PARAM_PWD = "pwd"

    @JvmStatic
    fun resolve(url: String, pwd: String? = null) {
      startActivity<ResolveFolderActivity>(PARAM_URL to url, PARAM_PWD to pwd)
    }
  }

  private var _downloadService: DownloadService? = null
  private val downloadService get() = _downloadService!!

  override fun init() {
    super.init()
    bindService(Intent(this, DownloadService::class.java), this, BIND_AUTO_CREATE)
  }

  override fun initData() {

  }

  override fun initView() {
    setTitle(getString(R.string.title_resolve_folder))
    actionRight.text = "网站中打开"
    actionRight.setOnClickListener {
      val url = intent.getStringExtra(PARAM_URL) ?: return@setOnClickListener
      val pwd = intent.getStringExtra(PARAM_PWD)
      val id = url.substringAfterLast("/")
      val webUrl = LanzouApplication.SHARE_FOLDER_URL + id + "/${pwd ?: ""}"
      WebActivity.actionStart(webUrl)
    }

    binding.folderRv.fitNavigationBar()
    binding.folderRv.divider {
      includeVisible = true
      orientation = DividerOrientation.GRID
      setDivider(10, true)
    }.setup {
      setAnimation(AnimationType.SLIDE_RIGHT)
      addType<LanzouShareFolderModel>(R.layout.item_grid_share_file)

      R.id.item_card.onClick {
        val model = getModel<LanzouShareFolderModel>()
        MaterialAlertDialogBuilder(this@ResolveFolderActivity)
          .setTitle("下载")
          .setMessage(model.fileName)
          .setPositiveButton("确认") { _, _ ->
            val url = "https://www.lanzoui.com/${model.fileId}"
            _downloadService?.addDownload(url, model.fileName, null)
          }
          .setNegativeButton("关闭", null)
          .show()
      }
    }

    binding.state.onRefresh {
      scope {
        val url = intent.getStringExtra(PARAM_URL) ?: throw IllegalArgumentException("分享地址为空")
        val pwd = intent.getStringExtra(PARAM_PWD)
        val files = LanzouRepository.getShareFolders(url, pwd)
        binding.folderRv.models = files
      }
    }.showLoading()
  }

  override fun onServiceConnected(p0: ComponentName, p1: IBinder?) {
    _downloadService = (p1 as DownloadService.DownloadBinder).service
  }

  override fun onServiceDisconnected(p0: ComponentName?) {
    _downloadService = null
  }

  override fun onDestroy() {
    super.onDestroy()
    unbindService(this)
    _downloadService = null
  }

}