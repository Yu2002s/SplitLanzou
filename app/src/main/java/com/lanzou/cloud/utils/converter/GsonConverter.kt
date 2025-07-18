package com.lanzou.cloud.utils.converter

import com.drake.net.convert.JSONConvert
import org.json.JSONObject
import java.lang.reflect.Type

/**
 * 使用 gson 对响应体进行反序列化
 */
class GsonConverter : JSONConvert(success = "200") {

  companion object {
    // private val gson = GsonBuilder().serializeNulls().create()
  }

  override fun <R> String.parseBody(succeed: Type): R? {
    val body = try {
      JSONObject(this).getString("data")
    } catch (e: Exception) {
      this
    }
    return null
    // return gson.fromJson<R>(body, succeed)
  }
}
