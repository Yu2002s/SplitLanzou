package com.lanzou.cloud.model

import kotlinx.serialization.Serializable

@Serializable
data class BaseLanzouResponse(
  val zt: Int,
  val text: String,
  val info: String
)