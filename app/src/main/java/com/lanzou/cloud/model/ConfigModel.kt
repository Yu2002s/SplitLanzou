package com.lanzou.cloud.model

import kotlinx.serialization.Serializable

@Serializable
data class ConfigModel(
  val download: Download = Download(),
) {

  @Serializable
  data class Download(val provider: List<Provider> = listOf()) {

    @Serializable
    data class Provider(val id: String, val name: String, val url: String)

  }
}