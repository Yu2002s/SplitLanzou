package com.lanzou.cloud.utils.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 刷新 token 拦截器
 * （由于后端响应码无法修改，此拦截器弃用）
 */
class RefreshTokenInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val response = chain.proceed(request)
    return synchronized(RefreshTokenInterceptor::class.java) {
      /*if (response.code == 401 && UserUtils.isLogin() && !request.url.encodedPath.contains(Api.REFRESH_TOKEN)) {
        val tokenInfo = Net.get(Api.REFRESH_TOKEN) {
          // addQuery("refreshToken", UserUtils.getRefreshToken())
        }.execute<UserBaseInfoModel>() // 同步请求token
        // 重新赋值登录信息（只需修改token信息即可，这里全部修改）
        UserUtils.login(tokenInfo)
        chain.proceed(request)
      } else {
        response
      }*/
      response
    }
  }
}