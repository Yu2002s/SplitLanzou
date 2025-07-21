package com.lanzou.cloud.ui.dialog

import android.content.Context
import com.drake.tooltip.toast
import com.lanzou.cloud.base.BaseEditDialog

class FileMkdirDialog(context: Context, private val path: String) : BaseEditDialog(context) {

  init {
    setTitle("新建文件夹")
    hint = "请输入文件夹名称"

    setPositiveButton("新建") { dialog, _ ->
      if (editValue.isEmpty()) {
        toast("请输入文件夹名称")
        return@setPositiveButton
      }
      onConfirm?.invoke(editValue)
    }

    setNegativeButton("关闭", null)
  }
}