package com.lanzou.cloud.event

import com.lanzou.cloud.enums.LayoutPosition

interface OnLayoutChangeListener {

  /**
   * 当布局改变时进行监听
   *
   * @param positon 当前布局所处的位置
   */
  fun onLayoutChange(positon: LayoutPosition)

}