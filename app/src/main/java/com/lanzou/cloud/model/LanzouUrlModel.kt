package com.lanzou.cloud.model


import com.lanzou.cloud.LanzouApplication
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URLEncoder

@Serializable
data class LanzouUrlModel(
  @SerialName("pwd")
  var pwd: String = "",
  @SerialName("onof")
  var hasPwd: Int = 0,
  @SerialName("f_id")
  var fileId: String = "",
  @SerialName("taoc")
  var taoc: String = "",
  @SerialName("is_newd")
  var host: String = ""
) {

  val shareUrl get() = "$host/$fileId"

  val shareText: String
    get() {
      var str = "分享地址: $shareUrl"
      if (hasPwd == 1) str += "\n文件密码: $pwd"
      return str
    }

  val customShareUrl get() = "${LanzouApplication.SHARE_URL2}$fileId"

  val customShareText: String
    get() {
      var str = "分享地址: $customShareUrl"
      if (hasPwd == 1) str += "/$pwd"
      return str
    }

  val downloadUrl: String
    get() {
      var str = "${LanzouApplication.API_URL}/parser?url=${URLEncoder.encode(shareUrl)}"
      if (hasPwd == 1) {
        str += "&pwd=$pwd"
      }
      return str
    }
}