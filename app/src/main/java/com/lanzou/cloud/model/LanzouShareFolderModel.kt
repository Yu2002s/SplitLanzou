package com.lanzou.cloud.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LanzouShareFolderModel(
  @SerialName("fileName")
  var fileName: String = "",
  @SerialName("fileId")
  var fileId: String = "",
  @SerialName("fileIcon")
  var fileIcon: String? = null,
  @SerialName("size")
  var size: Long = 0,
  @SerialName("sizeStr")
  var sizeStr: String = "",
  @SerialName("fileType")
  var fileType: String = "",
  @SerialName("filePath")
  var filePath: String? = null,
  @SerialName("createTime")
  var createTime: String = "",
  @SerialName("updateTime")
  var updateTime: String? = null,
  @SerialName("createBy")
  var createBy: String? = null,
  @SerialName("description")
  var description: String? = null,
  @SerialName("downloadCount")
  var downloadCount: String = "",
  @SerialName("panType")
  var panType: String = "",
  @SerialName("parserUrl")
  var parserUrl: String = "",
  // @SerialName("extParameters")
  // var extParameters: String = Any()
)