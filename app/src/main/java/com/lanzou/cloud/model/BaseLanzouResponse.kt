package com.lanzou.cloud.model

import kotlinx.serialization.Serializable

@Serializable
data class BaseLanzouResponse(
  val zt: Int = 0,
  val text: String? = null,
  val info: String? = null
)