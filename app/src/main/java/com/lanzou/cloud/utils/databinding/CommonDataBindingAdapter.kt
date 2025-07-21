package com.lanzou.cloud.utils.databinding

import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.material.button.MaterialButton
import com.lanzou.cloud.R
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.utils.FileUtils

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

  @JvmStatic
  @BindingAdapter("fileIcon")
  fun setFileIcon(view: ImageView, fileInfo: FileInfoModel) {
    val ext = fileInfo.extension
    if (ext == "apk" && fileInfo.downloadCount == 0 && fileInfo.path.isNotEmpty()) {
      Glide.with(view)
        .load(fileInfo)
        .placeholder(R.drawable.ic_apk)
        .into(view)
    } else if (FileUtils.isMediaFile(ext) && fileInfo.path.isNotEmpty()) {
      Glide.with(view)
        .load(fileInfo.path)
        .placeholder(R.drawable.ic_file)
        .into(view)
    } else {
      view.setImageResource(FileUtils.getIcon(ext))
    }
  }

  @JvmStatic
  @BindingAdapter("android:textColor")
  fun setTextColor(view: TextView, @ColorRes color: Int) {
    val colorRes = ContextCompat.getColor(view.context, color)
    view.setTextColor(colorRes)
  }
}