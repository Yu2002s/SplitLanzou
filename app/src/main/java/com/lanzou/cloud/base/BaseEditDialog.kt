package com.lanzou.cloud.base

import android.content.Context
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanzou.cloud.R
import com.lanzou.cloud.databinding.DialogEditBaseBinding

open class BaseEditDialog(context: Context) : MaterialAlertDialogBuilder(context) {

  protected val binding: DialogEditBaseBinding

  var editValue: String = ""
    get() = binding.editKey.text?.trim()?.toString() ?: ""
    set(value) {
      field = value
      binding.editKey.setText(value)
    }

  var hint: CharSequence = ""
    set(value) {
      field = value
      binding.editKey.hint = value
    }

  var onConfirm: ((String?) -> Unit)? = null

  init {
    val inflater = LayoutInflater.from(context)
    binding = DataBindingUtil.inflate(inflater, R.layout.dialog_edit_base, null, false)
    setView(binding.root)

    binding.root.post {
      binding.editKey.requestFocus()
    }
  }

}