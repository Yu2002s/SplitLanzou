@file:Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate")

package com.lanzou.cloud.utils.converter

import com.drake.net.NetConfig
import com.drake.net.convert.NetConverter
import com.drake.net.exception.ConvertException
import com.drake.net.exception.RequestParamsException
import com.drake.net.exception.ResponseException
import com.drake.net.exception.ServerResponseException
import com.drake.net.request.kType
import com.lanzou.cloud.model.BaseLanzouResponse
import com.lanzou.cloud.utils.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.lang.reflect.Type
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * 处理请求的序列化和反序列化
 */
class SerializationConverter(
  val success: Array<String> = arrayOf("1", "2"), // 成功的状态码
  val code: String = "zt", // code 字段名
  val message: String = "info", // message 字段名
  val data: String = "text"
) : NetConverter {

  companion object {

    private const val TAG = "SerializationConverter"
  }

  override fun <R> onConvert(succeed: Type, response: Response): R? {
    try {
      return NetConverter.onConvert(succeed, response)
    } catch (_: ConvertException) {
      val code = response.code
      when {
        code in 200..299 -> { // 请求成功
          val bodyString = response.body?.string() ?: return null
          val kType = response.request.kType
            ?: throw ConvertException(response, "Request does not contain KType")
          return try {
            val json = JSONObject(bodyString) // 获取JSON中后端定义的错误码和错误信息
            val srvCode = json.getString(this.code)
            when {
              success.contains(srvCode) -> {
                if (!json.has(data)) {
                  return null
                }
                if (kType == typeOf<BaseLanzouResponse>()) {
                  return bodyString.parseBody(kType)
                }
                json.getString(data).parseBody(kType)
              }

              else -> {
                val errorMessage = json.optString(
                  message,
                  NetConfig.app.getString(com.drake.net.R.string.no_error_message)
                )
                throw ResponseException(response, errorMessage, tag = srvCode) // 将业务错误码作为tag传递
              }
            }
          } catch (_: JSONException) { // 固定格式JSON分析失败直接解析JSON
            bodyString.parseBody(kType)
          }
        }

        code in 400..499 -> throw RequestParamsException(response, code.toString()) // 请求参数错误
        code >= 500 -> throw ServerResponseException(response, code.toString()) // 服务器异常错误
        else -> throw ConvertException(response, message = "Http status code not within range")
      }
    }
  }

  fun <R> String.parseBody(succeed: KType): R? {
    return json.decodeFromString(Json.serializersModule.serializer(succeed), this) as R
  }
}