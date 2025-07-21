package com.lanzou.cloud.ui.dialog

import android.content.Context
import com.drake.tooltip.toast
import com.lanzou.cloud.base.BaseEditDialog

class FileSearchDialog(context: Context) : BaseEditDialog(context) {

  init {
    setTitle("搜索")
    hint = "请输入关键字"

    setNeutralButton("重置") { dialog, _ ->
      onConfirm?.invoke(null)
    }

    setPositiveButton("搜索") { dialog, _ ->
      if (editValue.isBlank()) {
        toast("请输入关键字")
        return@setPositiveButton
      }
      onConfirm?.invoke(editValue)
    }

    setNegativeButton("关闭", null)
  }

}