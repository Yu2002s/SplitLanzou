package com.lanzou.cloud.base

import androidx.annotation.LayoutRes
import androidx.databinding.ViewDataBinding
import com.drake.engine.base.EngineActivity

abstract class BaseActivity<B : ViewDataBinding>(@LayoutRes contentResId: Int = 0) :
  EngineActivity<B>(contentResId) {


}