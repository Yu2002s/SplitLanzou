package com.lanzou.cloud.utils.interceptor

import com.drake.net.interceptor.RequestInterceptor
import com.drake.net.request.BaseRequest
import com.lanzou.cloud.network.Repository

/**
 * App 全局请求拦截器
 */
class AppRequestInterceptor : RequestInterceptor {

  override fun interceptor(request: BaseRequest) {
    val repository = Repository.getInstance()
    repository.cookie?.let {
      request.addHeader("Cookie", it)
      request.addQuery("uid", repository.savedUser?.uid)
    }
  }
}