package com.lanzou.cloud.utils.databinding

import android.graphics.Color
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.material.button.MaterialButton

/**
 * 通用 DataBinding 适配器
 */
object CommonDataBindingAdapter {

  @JvmStatic
  @BindingAdapter("imageUrl")
  fun loadImage(view: ImageView, url: String?) {
    if (!url.isNullOrEmpty()) {
      Glide.with(view.context)
        .load(url)
        .placeholder(Color.GRAY.toDrawable())
        .into(view)
    }
  }

  @JvmStatic
  @BindingAdapter("avatar")
  fun setAvatar(view: ImageView, url: String?) {
    if (!url.isNullOrEmpty()) {
      Glide.with(view.context)
        .load(url)
        .placeholder(Color.GRAY.toDrawable())
        .transform(CircleCrop())
        .into(view)
    }
  }

  @JvmStatic
  @BindingAdapter("android:checked")
  fun setChecked(view: MaterialButton, checked: Boolean) {
    view.isChecked = checked
  }
}