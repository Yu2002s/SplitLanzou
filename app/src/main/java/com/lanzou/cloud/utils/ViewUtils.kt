package com.lanzou.cloud.utils

import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.bumptech.glide.Glide
import java.util.concurrent.TimeUnit


/*

    操作 View 相关的工具类

 */

/**
 * RecyclerView 懒加载图片
 */
fun RecyclerView.lazyLoadImg(): RecyclerView {
  this.addOnScrollListener(object : OnScrollListener() {
    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
      when (newState) {
        RecyclerView.SCROLL_STATE_SETTLING -> {
          Glide.with(this@lazyLoadImg.context).pauseRequests()
        }

        RecyclerView.SCROLL_STATE_IDLE -> {
          Glide.with(this@lazyLoadImg.context).resumeRequests()
        }
      }
    }
  })
  return this
}

/**
 * 让 View 自适应底部的导航栏
 */
fun View.fitNavigationBar(target: View? = null, oneSet: Boolean = false) {
  val targetView = target ?: this
  ViewCompat.setOnApplyWindowInsetsListener(targetView) { v, insets ->
    if (oneSet) {
      ViewCompat.setOnApplyWindowInsetsListener(targetView, null)
    }
    if (v is ViewGroup) {
      v.clipToPadding = false
    }
    val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
    v.updatePadding(bottom = bottom)
    insets
  }
}

fun View.fitStatusBar(target: View? = null, defaultPadding: Int = 0, oneSet: Boolean = false) {
  val targetView = target ?: this
  ViewCompat.setOnApplyWindowInsetsListener(targetView) { v, insets ->
    if (oneSet) {
      ViewCompat.setOnApplyWindowInsetsListener(targetView, null)
    }
    if (v is ViewGroup) {
      v.clipToPadding = false
    }
    val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
    v.updatePadding(top = top + defaultPadding.dp2px())
    insets
  }
}

/**
 * 视图节流点击
 */
fun View.throttleClick(
  interval: Long = 500,
  unit: TimeUnit = TimeUnit.MILLISECONDS,
  block: View.() -> Unit
) {
  setOnClickListener(ThrottleClickListener(interval, unit, block))
}

class ThrottleClickListener(
  private val interval: Long = 500,
  private val unit: TimeUnit = TimeUnit.MILLISECONDS,
  private var block: View.() -> Unit
) : View.OnClickListener {
  private var lastTime: Long = 0

  override fun onClick(v: View) {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastTime > unit.toMillis(interval)) {
      lastTime = currentTime
      block(v)
    }
  }
}

fun View?.startShakeByPropertyAnim(
  scaleSmall: Float = 0.9f,
  scaleLarge: Float = 1.1f,
  shakeDegrees: Float = 4f,
  duration: Long = 1000
) {
  if (this == null) {
    return
  }

  //先变小后变大
  val scaleXValuesHolder = PropertyValuesHolder.ofKeyframe(
    View.SCALE_X,
    Keyframe.ofFloat(0f, 1.0f),
    Keyframe.ofFloat(0.25f, scaleSmall),
    Keyframe.ofFloat(0.5f, scaleLarge),
    Keyframe.ofFloat(0.75f, scaleLarge),
    Keyframe.ofFloat(1.0f, 1.0f)
  )
  val scaleYValuesHolder = PropertyValuesHolder.ofKeyframe(
    View.SCALE_Y,
    Keyframe.ofFloat(0f, 1.0f),
    Keyframe.ofFloat(0.25f, scaleSmall),
    Keyframe.ofFloat(0.5f, scaleLarge),
    Keyframe.ofFloat(0.75f, scaleLarge),
    Keyframe.ofFloat(1.0f, 1.0f)
  )


  //先往左再往右
  val rotateValuesHolder = PropertyValuesHolder.ofKeyframe(
    View.ROTATION,
    Keyframe.ofFloat(0f, 0f),
    Keyframe.ofFloat(0.1f, -shakeDegrees),
    Keyframe.ofFloat(0.2f, shakeDegrees),
    Keyframe.ofFloat(0.3f, -shakeDegrees),
    Keyframe.ofFloat(0.4f, shakeDegrees),
    Keyframe.ofFloat(0.5f, -shakeDegrees),
    Keyframe.ofFloat(0.6f, shakeDegrees),
    Keyframe.ofFloat(0.7f, -shakeDegrees),
    Keyframe.ofFloat(0.8f, shakeDegrees),
    Keyframe.ofFloat(0.9f, -shakeDegrees),
    Keyframe.ofFloat(1.0f, 0f)
  )

  val objectAnimator = ObjectAnimator.ofPropertyValuesHolder(
    this,
    scaleXValuesHolder,
    scaleYValuesHolder,
    rotateValuesHolder
  )
  objectAnimator.duration = duration
  objectAnimator.start()
}