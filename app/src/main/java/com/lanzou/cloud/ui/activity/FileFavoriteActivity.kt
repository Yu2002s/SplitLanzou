package com.lanzou.cloud.ui.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.View
import com.drake.brv.item.ItemExpand
import com.drake.brv.utils.setup
import com.drake.engine.utils.ClipboardUtils
import com.drake.net.utils.scope
import com.drake.net.utils.scopeDialog
import com.drake.net.utils.withIO
import com.drake.tooltip.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanzou.cloud.R
import com.lanzou.cloud.base.BaseEditDialog
import com.lanzou.cloud.base.BaseToolbarActivity
import com.lanzou.cloud.databinding.ActivityFileFavoriteBinding
import com.lanzou.cloud.model.FavoriteItem
import com.lanzou.cloud.model.FileFavoritesModel
import com.lanzou.cloud.model.LanzouResolveFileModel
import com.lanzou.cloud.service.DownloadService
import com.lanzou.cloud.ui.dialog.FileFavoriteDialog
import org.litepal.LitePal
import org.litepal.extension.deleteAll
import org.litepal.extension.find
import org.litepal.extension.findAll

class FileFavoriteActivity :
  BaseToolbarActivity<ActivityFileFavoriteBinding>(R.layout.activity_file_favorite),
  ServiceConnection {

  private var _downloadService: DownloadService? = null

  override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
    _downloadService = (service as DownloadService.DownloadBinder).service
  }

  override fun onServiceDisconnected(name: ComponentName?) {

  }

  override fun init() {
    super.init()
    bindService(Intent(this, DownloadService::class.java), this, BIND_AUTO_CREATE)
  }

  override fun initData() {

  }

  override fun initView() {
    title = "我的收藏"
    actionRight.text = "添加"
    actionRight.setOnClickListener {
      BaseEditDialog(this).apply {
        setTitle("添加收藏夹")
        hint = "请输入名称"
        setPositiveButton("确认") { dialog, which ->
          if (editValue.isBlank()) {
            return@setPositiveButton
          }
          scopeDialog {
            withIO {
              val model = FileFavoritesModel(name = editValue)
              model.save()
            }
            toast("添加成功")
            binding.page.refresh()
          }
        }
        setNegativeButton("取消", null)
        show()
      }
    }
    binding.page.onRefresh {
      scope {
        val models = withIO {
          LitePal.findAll<FileFavoritesModel>().onEach {
            it.items.addAll(
              LitePal.where("filefavoritesmodel_id = ?", it.id.toString()).order("updateAt desc")
                .find<FavoriteItem>()
            )
          }
        }
        addData(models)
      }
    }.apply {
      setEnableLoadMore(false)
      showLoading()
    }

    binding.rvFavorite.setup {
      addType<FileFavoritesModel>(R.layout.item_favorite)
      addType<FavoriteItem>(R.layout.item_list_favorite)

      R.id.item.onFastClick {
        expandOrCollapse()
      }

      R.id.btn_delete.onClick {
        val model = getModel<FileFavoritesModel>()
        MaterialAlertDialogBuilder(this@FileFavoriteActivity)
          .setTitle("删除收藏夹")
          .setMessage("是否删除[${model.name}]收藏夹")
          .setPositiveButton("确定") { dialog, which ->
            scopeDialog {
              withIO {
                model.delete()
                LitePal.deleteAll<FavoriteItem>("filefavoritesmodel_id = ?", model.id.toString())
              }
              toast("删除成功")
              binding.page.refresh()
            }
          }
          .setNegativeButton("取消", null)
          .show()
      }

      R.id.btn_delete_favorite.onClick {
        val model = getModel<FavoriteItem>()
        scopeDialog {
          // LitePal.delete<FavoriteItem>(model.id)
          withIO {
            model.delete()
          }
          toast("删除成功")
          val parentPosition = findParentPosition()
          if (parentPosition != -1) {
            // 删除父item的嵌套分组数据
            (getModel<ItemExpand>(parentPosition).getItemSublist() as MutableList).remove(model)
            mutable.removeAt(layoutPosition)
            notifyItemRemoved(layoutPosition)
          }
          // binding.page.refresh()
        }
      }

      R.id.item_favorite.onClick {
        val model = getModel<FavoriteItem>()
        val items = arrayOf(
          "地址:" + model.url,
          "密码:${model.pwd}",
          "时间:${model.time}",
          "大小:${model.size}",
          "备注:${model.remark}"
        )
        val positiveButton = if (model.isFile) "下载" else "打开"
        MaterialAlertDialogBuilder(this@FileFavoriteActivity)
          .setTitle(model.name)
          .setItems(items) { dialog, which ->
            ClipboardUtils.copyText(items[which])
            toast("已复制${items[which]}")
          }
          .setPositiveButton(positiveButton) { dialog, which ->
            if (!model.isFile) {
              ResolveFolderActivity.resolve(model.url, model.pwd)
              return@setPositiveButton
            }
            _downloadService?.addDownload(model.url, model.name, model.pwd)
          }
          .setNeutralButton("编辑") { dialog, which ->
            FileFavoriteDialog(
              LanzouResolveFileModel(
                url = model.url,
                pwd = model.pwd,
                fileName = model.name,
                fileSize = model.size ?: "",
                shareTime = model.time,
                remark = model.remark ?: "",
                isFile = model.isFile
              )
            ).apply {
              onFavoriteClickListener = View.OnClickListener {
                this@FileFavoriteActivity.binding.page.refresh()
              }
              show(supportFragmentManager, "FileFavoriteDialog")
            }
          }
          .setNegativeButton("关闭", null)
          .show()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    unbindService(this)
    _downloadService = null
  }
}