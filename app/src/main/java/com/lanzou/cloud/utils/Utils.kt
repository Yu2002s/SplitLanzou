package com.lanzou.cloud.utils

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.drake.net.request.BodyRequest
import com.drake.net.request.MediaConst
import com.lanzou.cloud.LanzouApplication
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.RequestBody.Companion.toRequestBody


/*
     一些通用的方法
 */

/**
 * 显示土司
 */
fun String?.showToast(duration: Int = Toast.LENGTH_SHORT) {
  Toast.makeText(LanzouApplication.context, this, duration).show()
}

/**
 * 开启 activity
 * @param args 传递的参数
 */
inline fun <reified T> startActivity(
  vararg args: Pair<String, Any>,
  block: Intent.() -> Unit = {}
) {
  val context = LanzouApplication.context
  context.startActivity(Intent(context, T::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    this.block()
    args.forEach {
      when (val second = it.second) {
        is String -> putExtra(it.first, second)
        is Int -> putExtra(it.first, second)
        is Float -> putExtra(it.first, second)
        is Parcelable -> putExtra(it.first, second)
        is Boolean -> putExtra(it.first, second)
      }
    }
  })
}

/*fun BodyRequest.gson(vararg body: Pair<String, Any?>) {
  this.body = Gson().toJson(body.toMap()).toRequestBody(MediaConst.JSON)
}*/

/**
 * Bundle 中添加序列化参数
 */
inline fun <reified T : @Serializable Any> Bundle.putSerializable(key: String, value: T) {
  val jsonString = Json.encodeToString(value)
  putString(key, jsonString)
}

inline fun <reified T : @Serializable Any> Intent.putSerializable(key: String, value: T) {
  putExtra(key, Json.encodeToString(value))
}

/**
 * Bundle 中获取序列化参数
 */
inline fun <reified T : @Serializable Any> Bundle.getSerializableForKey(key: String): T? {
  val jsonString = getString(key) ?: return null
  return Json.decodeFromString<T>(jsonString)
}

/**
 * Fragment 中添加序列化参数
 */
inline fun <reified T : @Serializable Any> Fragment.setSerializableArguments(
  key: String,
  value: T
) {
  arguments = (arguments ?: Bundle()).apply {
    putSerializable(key, value)
  }
}

/**
 * Fragment 中获取序列化参数
 */
inline fun <reified T : @Serializable Any> Fragment.getSerializableArguments(key: String): T? {
  return arguments?.getSerializableForKey(key)
}

inline fun <reified T : @Serializable Any> Activity.setSerializableArguments(
  key: String,
  value: T
) {
  intent.extras?.putSerializable(key, value)
}

inline fun <reified T : @Serializable Any> Activity.getSerializableArguments(key: String): T? {
  return intent.extras?.getSerializableForKey(key)
}

/**
 * 全局使用的 json 对象
 */
val json = Json {
  encodeDefaults = true    // 序列化默认值
  ignoreUnknownKeys = true // 忽略未知属性名
  ignoreUnknownKeys = true // 数据类可以不用声明Json的所有字段
  coerceInputValues = true // 如果Json字段是Null则使用数据类字段默认值
}

/**
 * 扩展支持直接发送对象并转换为 json
 */
inline fun <reified T> BodyRequest.json(body: T) {
  this.body = json.encodeToString(body)
    .toRequestBody(MediaConst.JSON)
}

fun Long.formatBytes(): String {
  val units = arrayOf("B", "KB", "MB", "GB", "TB")
  if (this == 0L) return "0 B"
  var currentBytes = this.toDouble()
  var unitIndex = 0

  while (currentBytes >= 1024 && unitIndex < units.size - 1) {
    currentBytes /= 1024.0
    unitIndex++
  }

  return "%.2f %s".format(currentBytes, units[unitIndex])
}