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
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.model.OptionItem

class FileActionDialog(context: Context, positon: LayoutPosition = LayoutPosition.LEFT) :
  MaterialAlertDialogBuilder(context) {

  var onItemClick: ((Int) -> Unit)? = null

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

    binding.actionRv.dividerSpace(20, DividerOrientation.GRID).setup {
      addType<OptionItem>(R.layout.item_list_option)

      R.id.item_card.onClick {
        dialog.dismiss()
        onItemClick?.invoke(modelPosition)
      }
    }.models = listOf(
      if (positon == LayoutPosition.LEFT) OptionItem(
        R.drawable.outline_arrow_forward_24,
        "下载",
        0
      )
      else OptionItem(R.drawable.baseline_arrow_back_24, "上传", 0),
      OptionItem(R.drawable.baseline_delete_outline_24, "删除", 0),
      OptionItem(R.drawable.baseline_drive_file_rename_outline_24, "重命名", 0),
      OptionItem(R.drawable.baseline_settings_24, "详情", 0),
    )

    dialog.show()

  }

}