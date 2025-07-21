package com.lanzou.cloud.ui.fragment

import android.annotation.SuppressLint
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.models
import com.drake.brv.utils.mutable
import com.drake.brv.utils.setDifferModels
import com.drake.brv.utils.setup
import com.drake.engine.base.EngineFragment
import com.drake.net.utils.scope
import com.lanzou.cloud.R
import com.lanzou.cloud.databinding.FragmentFileBaseBinding
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.event.Backable
import com.lanzou.cloud.event.FileAction
import com.lanzou.cloud.event.OnFileNavigateListener
import com.lanzou.cloud.event.OnLayoutChangeListener
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.network.Repository
import com.lanzou.cloud.ui.activity.WebActivity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

abstract class FileFragment(private val layoutPosition: LayoutPosition = LayoutPosition.LEFT) :
  EngineFragment<FragmentFileBaseBinding>(R.layout.fragment_file_base), OnLayoutChangeListener,
  Backable, FileAction {

  protected val viewModel by viewModels<HomeViewModel>(ownerProducer = { requireParentFragment() })

  protected val mData = mutableListOf<FileInfoModel>()

  abstract suspend fun getData(page: Int): List<FileInfoModel>?

  private var isFirst = true

  override fun initData() {
    lifecycleScope.launch {
      viewModel.currentPositionFlow.collect {
        onLayoutChange(it)
      }
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun initView() {
    binding.m = viewModel
    binding.lifecycleOwner = this

    binding.fileRv.setup {
      addType<FileInfoModel>(R.layout.item_list_fileinfo)

      onChecked { position, checked, allChecked ->
        val model = getModel<FileInfoModel>(position)
        model.isChecked = checked
      }

      onToggle { position, toggleMode, end ->
        val model = getModel<FileInfoModel>(position)
        model.isCheckable = toggleMode
        if (this@FileFragment.layoutPosition == LayoutPosition.LEFT) {
          viewModel.toggleLeft(toggleMode)
        } else {
          viewModel.toggleRight(toggleMode)
        }
        if (!toggleMode) {
          checkedAll(false)
        }
      }

      R.id.item.onLongClick {
        if (!toggleMode) {
          toggle()
          setChecked(modelPosition, true)
        }
      }

      R.id.item.onFastClick {
        if (toggleMode) {
          val model = getModel<FileInfoModel>(modelPosition)
          setChecked(modelPosition, !model.isChecked)
          return@onFastClick
        }
        viewModel.focusPosition(this@FileFragment.layoutPosition)
        onItemClick(getModel(), modelPosition)
      }
    }

    binding.refresh.onRefresh {
      scope {
        val data = coroutineScope {
          getData(index)
        }
        if (index == 1) {
          mData.clear()
        }
        data?.let {
          mData.addAll(it)
        }
        addData(data) {
          isLoadMore(data)
        }
        onLoadEnd(data)
      }
    }

    binding.fileRv.setOnTouchListener { v, e ->
      viewModel.focusPosition(layoutPosition)
      return@setOnTouchListener false
    }

    binding.btnLogin.setOnClickListener {
      WebActivity.login()
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshLogin()
    if (!Repository.getInstance().isLogin) {
      return
    }
    if (isFirst) {
      isFirst = false
      binding.refresh.showLoading()
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    isFirst = true
  }

  override fun onLayoutChange(positon: LayoutPosition) {
    if (view != null) {
      binding.fileRv.layoutManager = when (positon) {
        LayoutPosition.RIGHT, LayoutPosition.MIDDLE -> LinearLayoutManager(requireContext())
        LayoutPosition.LEFT -> StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
      }
    }
  }

  protected open fun onItemClick(model: FileInfoModel, position: Int) {
    navigateTo(model, position)
  }

  override fun onBack(): Boolean {
    return true
  }

  protected fun navigateTo(fileInfoModel: FileInfoModel, position: Int) {
    val parentFragment = parentFragment ?: return
    if (parentFragment is OnFileNavigateListener) {
      parentFragment.navigate(fileInfoModel, position)
    }
  }

  protected open fun onLoadEnd(data: List<FileInfoModel>?) {}

  override fun onSearch(keyWorld: String?) {
    if (keyWorld.isNullOrBlank()) {
      binding.fileRv.models = mData
    } else {
      binding.fileRv.setDifferModels(mData.filter {
        it.name.lowercase().contains(keyWorld.lowercase())
      })
    }
  }

  override fun onMkdir(name: String, path: String) {
    val fileRv = binding.fileRv
    val position = if (fileRv.models.isNullOrEmpty()) 0 else 1
    fileRv.bindingAdapter.notifyItemInserted(position)
    fileRv.scrollToPosition(position)
  }

  override fun toggleMulti(isMultiMode: Boolean) {
    if (view == null) {
      return
    }
    binding.refresh.setEnableRefresh(!isMultiMode)
    binding.refresh.setEnableLoadMore(!isMultiMode)
    binding.fileRv.bindingAdapter.toggle(isMultiMode)
  }

  protected open fun isLoadMore(data: List<FileInfoModel>?): Boolean {
    return false
  }

  open fun getCurrentPath(): String? {
    return null
  }

  override fun getCheckedFiles(): List<FileInfoModel> {
    return mData.filter { it.isChecked && it.isFile }
  }

  override fun addFile(fileInfoModel: FileInfoModel) {
    addFile(getInsertPosition(), fileInfoModel)
  }

  override fun addFile(position: Int, fileInfoModel: FileInfoModel) {
    binding.fileRv.mutable.add(position, fileInfoModel)
    binding.fileRv.bindingAdapter.notifyItemInserted(position)
    binding.fileRv.scrollToPosition(position)
  }

  override fun addFiles(files: List<FileInfoModel>) {
    if (files.isEmpty()) {
      return
    }
    val position = getInsertPosition()
    binding.fileRv.mutable.addAll(position, files)
    binding.fileRv.bindingAdapter.notifyItemRangeInserted(position, files.size)
    binding.fileRv.post {
      scrollToPosition(position)
    }
  }

  protected fun scrollToPosition(position: Int) {
    val layoutManager = binding.fileRv.layoutManager
    // FIXME: 当切换layoutManager 后可能出现问题
    if (layoutManager is LinearLayoutManager) {
      layoutManager.scrollToPositionWithOffset(position, 0)
    } else if (layoutManager is StaggeredGridLayoutManager) {
      layoutManager.scrollToPositionWithOffset(position, 0)
    }
  }

  /**
   * 获取准确的插入位置
   */
  @Suppress("unchecked_cast")
  protected open fun getInsertPosition(): Int {
    val fileRv = binding.fileRv
    val models = fileRv.models as? List<FileInfoModel>
    if (models.isNullOrEmpty()) {
      return 0
    }
    return models.indexOfLast { it.isDirectory } + 1
  }

  override fun refresh() {
    binding.refresh.refresh()
  }
}