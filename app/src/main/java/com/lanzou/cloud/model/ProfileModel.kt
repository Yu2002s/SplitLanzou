package com.lanzou.cloud.model

import com.lanzou.cloud.R

data class ProfileModel(
  val name: String,
  val value: String,
  val clickable: Boolean = false
) {

  val rightIcon get() = if (clickable) R.drawable.baseline_chevron_right_24 else 0

}
