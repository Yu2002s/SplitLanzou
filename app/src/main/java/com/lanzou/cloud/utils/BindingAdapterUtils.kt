package com.lanzou.cloud.utils

import androidx.recyclerview.widget.RecyclerView
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.mutable

/**
 * 添加 Model
 *
 * @param position 对应的 layoutPosition（包含Header）
 */
fun <T> RecyclerView.addModel(position: Int, model: T): Int {
  bindingAdapter.let {
    val realPosition = position + it.headerCount
    mutable.add(position, model)
    it.notifyItemInserted(realPosition)
    return position
  }
}

fun RecyclerView.removeModel(position: Int, change: Boolean = true): Int {
  bindingAdapter.let {
    val realPosition = position + it.headerCount
    mutable.removeAt(position)
    if (change) {
      it.notifyItemRemoved(realPosition)
    }
    return position
  }
}

/**
 * 删除 Models
 */
fun RecyclerView.removeModels(
  positions: List<Int>,
  clear: Boolean = false,
  cb: (Int) -> Unit
) {
  positions.sortedDescending().forEach {
    bindingAdapter.let { adapter ->
      val position = it - adapter.headerCount
      cb(position)
      mutable.removeAt(position)
      adapter.notifyItemRemoved(it)
    }
  }
  if (clear) {
    bindingAdapter.checkedPosition.clear()
  }
}

suspend fun RecyclerView.removeModelsSuspend(
  positions: List<Int>,
  clear: Boolean = false,
  cb: suspend (Int) -> Unit
) {
  positions.sortedDescending().forEach {
    bindingAdapter.let { adapter ->
      val position = it - adapter.headerCount
      cb(position)
      mutable.removeAt(position)
      adapter.notifyItemRemoved(it)
    }
  }
  if (clear) {
    bindingAdapter.checkedPosition.clear()
  }
}

fun RecyclerView.updateModel(position: Int) {
  bindingAdapter.let {
    it.notifyItemChanged(position + it.headerCount)
  }
}