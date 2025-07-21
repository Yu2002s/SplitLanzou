package com.lanzou.cloud.model

import androidx.databinding.BaseObservable

data class EditPasswordModel(
  var password: String = "",
  var newPassword: String = "",
  var confirmPassword: String = "",
) : BaseObservable() {

  fun validate(): String? {
    if (password.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
      return "密码或新密码不能为空"
    }

    if (newPassword != confirmPassword) {
      return "新密码和再次输入的密码不一致"
    }

    return null
  }

}
