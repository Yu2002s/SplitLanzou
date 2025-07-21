package com.lanzou.cloud.utils;

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.WindowManager
import com.lanzou.cloud.LanzouApplication


/*

    显示相关的工具类

 */

/**
 * dp转像素
 */
fun Int.dp2px(context: Context = LanzouApplication.context): Int {
  return (context.resources.displayMetrics.density * this + 0.5).toInt()
}

/**
 * 获取窗口的宽度
 */
@JvmName("getWindowWidthWithCtx")
fun Context.getWindowWidth(): Int {
  if (this is Activity) {
    return getWindowWidth()
  }
  return getWindowWidth(this)
}

fun WindowManager.getWindowWidth(): Int {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    currentWindowMetrics.bounds.width()
  } else {
    defaultDisplay.width
  }
}

fun WindowManager.getWindowHeight(): Int {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    currentWindowMetrics.bounds.height()
  } else {
    defaultDisplay.height
  }
}

/**
 * dialog中获取窗口的宽度
 */
fun Dialog.getWindowWidth(): Int {
  return this.window?.windowManager?.getWindowWidth() ?: 0
}

fun Dialog.getWindowHeight(): Int {
  return this.window?.windowManager?.getWindowHeight() ?: 0
}

fun Activity.getWindowWidth(): Int {
  return this.windowManager.getWindowWidth()
}

fun Activity.getWindowHeight(): Int {
  return this.windowManager.getWindowHeight()
}

fun getWindowWidth() = getWindowWidth(null)

fun getWindowWidth(ctx: Context? = null): Int {
  val context = ctx ?: LanzouApplication.context
  val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  return windowManager.getWindowWidth()
}

fun getWindowHeight(): Int {
  val context = LanzouApplication.context
  val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  return windowManager.getWindowHeight()
}

/**
 * 是否为深色模式
 */
val isDarkMode: Boolean
  get() {
    val nightModeFlags =
      LanzouApplication.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
  }
