package com.lanzou.cloud.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.lanzou.cloud.LanzouApplication

object SpUtils {

  const val DEFAULT_KEY = "com.lanzou.cloud_preferences"

  private val spKey = mutableMapOf<String, SharedPreferences>()

  fun getSp(name: String): SharedPreferences {
    if (name !in spKey) {
      spKey[name] = LanzouApplication.context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }
    return spKey[name]!!
  }

  @Suppress("UNCHECKED_CAST")
  fun <T> getOrDefault(spName: String, key: String, defaultValue: T) = with(getSp(spName)) {
    val res: Comparable<*>? = when (defaultValue) {
      is String -> getString(key, defaultValue)
      is Int -> getInt(key, defaultValue)
      is Float -> getFloat(key, defaultValue)
      is Long -> getLong(key, defaultValue)
      is Boolean -> getBoolean(key, defaultValue)
      else -> null
    }
    res as? T
  }

  /**
   * 可获取到int类型
   */
  inline fun <reified T> String.get(key: String, defaultValue: T? = null) = with(getSp(this)) {
    val res: Comparable<*>? = when (T::class) {
      String::class -> getString(key, defaultValue as? String)
      Int::class -> {
        val value = getInt(key, (defaultValue as? Int) ?: 0)
        // 对于Int类型进行空处理
        if (defaultValue == null && value == 0) null else value
      }

      Long::class -> {
        val value = getLong(key, (defaultValue as? Long) ?: 0)
        if (defaultValue == null && value == 0L) null else value
      }

      Float::class -> getFloat(key, (defaultValue as? Float) ?: 0f)
      Boolean::class -> getBoolean(key, (defaultValue as? Boolean) ?: false)
      else -> null
    }
    res as? T
  }

  fun <T> String.getOrDefaultNumber(key: String, default: T) = getOrDefault(this, key, default)!!

  fun clear(spName: String) {
    getSp(spName).edit().clear().apply()
  }

  inline fun <reified T> T.put(spName: String, key: String) = with(getSp(spName).edit()) {
    when (T::class) {
      String::class -> putString(key, this@put as String)
      Int::class -> putInt(key, this@put as Int)
      Float::class -> putFloat(key, this@put as Float)
      Boolean::class -> putBoolean(key, this@put as Boolean)
      Long::class -> putLong(key, this@put as Long)
      else -> throw IllegalStateException()
    }
    apply()
  }

  fun <T> put(spName: String, key: String, value: T) = with(getSp(spName).edit()) {
    when (value) {
      is String -> putString(key, value)
      is Int -> putInt(key, value)
      is Long -> putLong(key, value)
      is Float -> putFloat(key, value)
      is Boolean -> putBoolean(key, value)
    }
    apply()
  }

  infix fun <V> String.put(value: V) {
    put(DEFAULT_KEY, this, value)
  }

  inline fun <reified T> String.get(default: T? = null): T? {
    return DEFAULT_KEY.get(this, default)
  }

  inline fun <reified T> String.get() = get<T>(null)

  inline fun <reified T> String.getRequired(default: T): T {
    return DEFAULT_KEY.get(this, default)!!
  }

  fun String.remove() = DEFAULT_KEY.remove(this)

  fun String.remove(key: String) = getSp(this).edit { remove(key) }
}