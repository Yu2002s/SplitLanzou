package com.lanzou.cloud.ui.dialog

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import com.drake.net.utils.scopeNet
import com.drake.tooltip.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanzou.cloud.R
import com.lanzou.cloud.databinding.DialogEditPwdBinding
import com.lanzou.cloud.model.EditPasswordModel
import com.lanzou.cloud.network.LanzouRepository
import com.lanzou.cloud.network.Repository

class EditPasswordDialog(context: Context) : MaterialAlertDialogBuilder(context) {

  init {

    val inflater = LayoutInflater.from(context)

    val editPwdBinding =
      DataBindingUtil.inflate<DialogEditPwdBinding>(inflater, R.layout.dialog_edit_pwd, null, false)

    setTitle("修改密码")
    setView(editPwdBinding.root)

    val editPasswordModel = EditPasswordModel()
    editPwdBinding.m = editPasswordModel

    val dialog = create()
    dialog.show()

    editPwdBinding.btnSubmit.setOnClickListener {
      val errorMsg = editPasswordModel.validate()
      if (errorMsg != null) {
        toast(errorMsg)
        return@setOnClickListener
      }
      scopeNet {
        val result = LanzouRepository
          .editPassword(editPasswordModel.password, editPasswordModel.newPassword)
        if (!result) {
          toast("修改失败，请检查密码是否正确或符合要求")
        } else {
          toast("修改成功，请重新登录")
          Repository.getInstance().logout()
          dialog.dismiss()
          (context as Activity).finish()
        }
      }
    }

  }

}