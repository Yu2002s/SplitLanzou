package com.lanzou.cloud.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.dividerSpace
import com.drake.brv.utils.setup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanzou.cloud.R
import com.lanzou.cloud.databinding.DialogFileActionBaseBinding
import com.lanzou.cloud.enums.FilePageType
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.model.OptionItem

class FileActionDialog(
  context: Context,
  layoutPosition: LayoutPosition,
  current: FilePageType,
  target: FilePageType
) :
  MaterialAlertDialogBuilder(context) {

  var onItemClick: ((String) -> Unit)? = null

  init {

    val inflater = LayoutInflater.from(context)

    val binding = DataBindingUtil.inflate<DialogFileActionBaseBinding>(
      inflater,
      R.layout.dialog_file_action_base,
      null,
      false
    )

    setView(binding.root)

    val dialog = create()

    val options = mutableListOf(
      OptionItem(R.drawable.baseline_share_24, "分享", 0),
      OptionItem(R.drawable.baseline_drive_file_rename_outline_24, "重命名", 0),
      OptionItem(R.drawable.baseline_delete_outline_24, "删除", 0),
      OptionItem(R.drawable.baseline_settings_24, "详情", 0),
    )

    val icon = getOptionIcon(layoutPosition)
    // 两边都是远程页面
    if (current == FilePageType.REMOTE && current == target) {
      options.add(0, OptionItem(icon, "移动", 0))
    } else if (current == FilePageType.LOCAL && current == target) {
      options.add(0, OptionItem(R.drawable.baseline_content_copy_24, "复制", 0))
      options.add(0, OptionItem(icon, "移动", 0))
    } else if (current == FilePageType.LOCAL && target == FilePageType.REMOTE) {
      options.add(0, OptionItem(icon, "上传", 0))
    } else {
      options.add(0, OptionItem(icon, "下载", 0))
    }

    if (current == FilePageType.REMOTE) {
      options.add(OptionItem(R.drawable.baseline_favorite_24, "收藏", 0))
    }

    binding.actionRv.dividerSpace(20, DividerOrientation.GRID).setup {
      addType<OptionItem>(R.layout.item_list_option)

      R.id.item_card.onClick {
        dialog.dismiss()
        onItemClick?.invoke(getModel<OptionItem>().title)
      }
    }.models = options
    dialog.show()
  }

  private fun getOptionIcon(position: LayoutPosition): Int {
    if (position == LayoutPosition.LEFT) {
      return R.drawable.outline_arrow_forward_24
    }
    return R.drawable.baseline_arrow_back_24
  }
}