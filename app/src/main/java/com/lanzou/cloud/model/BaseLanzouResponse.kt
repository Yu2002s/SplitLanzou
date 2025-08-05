package com.lanzou.cloud.model

import kotlinx.serialization.Serializable

/**
 * 基础返回对象
 */
@Serializable
data class BaseLanzouResponse(
  /**
   * 状态码 1：成功，0：失败
   */
  val zt: Int = 0,
  /**
   * 返回成功内容
   */
  val text: String? = null,
  /**
   * 返回失败信息
   */
  val info: String? = null
)