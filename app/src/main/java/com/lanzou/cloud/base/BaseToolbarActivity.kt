package com.lanzou.cloud.base

import androidx.annotation.LayoutRes
import androidx.databinding.ViewDataBinding
import com.drake.engine.base.EngineToolbarActivity

abstract class BaseToolbarActivity<B : ViewDataBinding>(@LayoutRes contentResId: Int = 0) :
  EngineToolbarActivity<B>(contentResId) {

}