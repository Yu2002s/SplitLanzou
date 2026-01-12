package com.lanzou.cloud.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.drake.engine.utils.AppUtils
import com.lanzou.cloud.LanzouApplication

/**
 * 对 SharePreference 操作的工具类
 */
object SpUtils {

  /**
   * SharePreference 的默认键
   */
  val DEFAULT_KEY: String = AppUtils.getAppPackageName() + "_preferences"

  /**
   * key map 集合
   */
  private val spKeys = mutableMapOf<String, SharedPreferences>()

  /**
   * 获取指定的 SharePreference 实例
   */
  fun getSp(name: String): SharedPreferences {
    if (name !in spKeys) {
      spKeys[name] = LanzouApplication.context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }
    return spKeys[name]!!
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
   * 通过 key 获取到指定的 value
   *
   * @param key 键名
   * @param defaultValue 默认值，可空
   * @return 获取到的值， 当默认值为 null 时，获取到数字为 0 则为 null
   */
  inline fun <reified T> String.get(key: String, defaultValue: T? = null) = with(getSp(this)) {
    when (T::class) {
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
    } as? T
  }

  /**
   * 清除指定的 SharePreference 数据
   *
   * @param spName 名称
   */
  fun clear(spName: String) {
    getSp(spName).edit { clear() }
  }

  inline fun <reified T> T.put(spName: String, key: String) = getSp(spName).edit {
    when (T::class) {
      String::class -> putString(key, this@put as String)
      Int::class -> putInt(key, this@put as Int)
      Float::class -> putFloat(key, this@put as Float)
      Boolean::class -> putBoolean(key, this@put as Boolean)
      Long::class -> putLong(key, this@put as Long)
      else -> throw IllegalStateException()
    }
  }

  /**
   * 通过 spName，key 设置值
   *
   * @param spName SharePreference 名称
   * @param key 指定键名
   * @param value 保存的值
   */
  fun <T> put(spName: String, key: String, value: T) = getSp(spName).edit {
    when (value) {
      is String -> putString(key, value)
      is Int -> putInt(key, value)
      is Long -> putLong(key, value)
      is Float -> putFloat(key, value)
      is Boolean -> putBoolean(key, value)
    }
  }

  /**
   * 默认的 SP 设置值
   *
   * @param value 值
   * @sample
   * ```kotlin
   * key put 'hello'
   * ```
   */
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