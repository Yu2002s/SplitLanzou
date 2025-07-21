package com.lanzou.cloud.event

import com.lanzou.cloud.enums.LayoutPosition

interface OnLayoutChangeListener {

  fun onLayoutChange(positon: LayoutPosition)

}