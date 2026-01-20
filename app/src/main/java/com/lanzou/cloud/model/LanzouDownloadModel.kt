package com.lanzou.cloud.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LanzouDownloadModel(
  @SerialName("zt")
  val status: Int = 0,
  val dom: String = "",
  val url: String = "",
  val inf: String = ""
)