package com.lanzou.cloud.model

import com.lanzou.cloud.data.User

data class UserInfoModel(
  var user: User? = null,
  var phone: String = "",
  var level: String = "",
  var permission: String = "",

  var showProfile: Boolean = true,
) {

  val avatar get() = username.substring(0, 1)

  val isLogin get() = user != null

  val username: String get() = if (isLogin) user!!.username else "请先登录"

}
