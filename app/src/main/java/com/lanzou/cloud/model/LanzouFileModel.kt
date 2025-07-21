package com.lanzou.cloud.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LanzouFileModel(
  @SerialName("icon")
  var icon: String = "",
  @SerialName("id")
  var id: String = "",
  @SerialName("name_all")
  var nameAll: String = "",
  @SerialName("name")
  var name: String = "",
  @SerialName("size")
  var size: String = "",
  @SerialName("time")
  var time: String = "",
  @SerialName("downs")
  var downs: String = "",
  @SerialName("onof")
  var onof: String = "",
  @SerialName("is_lock")
  var isLock: String = "",
  @SerialName("filelock")
  var filelock: String = "",
  @SerialName("is_copyright")
  var isCopyright: Int = 0,
  @SerialName("is_bakdownload")
  var isBakdownload: Int = 0,
  @SerialName("bakdownload")
  var bakdownload: String = "",
  @SerialName("is_des")
  var isDes: Int = 0,
  @SerialName("is_ico")
  var isIco: Int = 0,
  @SerialName("folderlock")
  var folderlock: String = "",
  @SerialName("fol_id")
  var folId: String = "",
  @SerialName("folder_des")
  var folderDes: String = ""
)