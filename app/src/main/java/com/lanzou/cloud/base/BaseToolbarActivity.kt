package com.lanzou.cloud.base

import android.graphics.Color
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.databinding.ViewDataBinding
import com.drake.engine.base.EngineToolbarActivity
import com.drake.engine.swipeback.SwipeBackHelper

abstract class BaseToolbarActivity<B : ViewDataBinding>(@LayoutRes contentResId: Int = 0) :
  EngineToolbarActivity<B>(contentResId) {

  private var swipeBackHelper: SwipeBackHelper? = null

  /**
   * 关闭侧滑
   */
  var swipeEnable = true
    set(value) {
      field = value
      swipeBackHelper?.setEnable(field)
    }

  override fun init() {
    swipeBackHelper = SwipeBackHelper(this)
    swipeBackHelper?.setBackgroundColor(Color.WHITE)
    enableEdgeToEdge()
    ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
      val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
      v.updatePadding(top = top)
      insets
    }
    super.init()
  }

  override fun dispatchTouchEvent(event: MotionEvent): Boolean {
    swipeBackHelper?.dispatchTouchEvent(event)
    return super.dispatchTouchEvent(event)
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    swipeBackHelper?.onTouchEvent(event)
    return super.onTouchEvent(event)
  }
}