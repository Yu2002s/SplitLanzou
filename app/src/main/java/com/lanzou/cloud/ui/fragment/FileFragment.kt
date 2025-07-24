package com.lanzou.cloud.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.drake.brv.BindingAdapter.BindingViewHolder
import com.drake.brv.annotaion.AnimationType
import com.drake.brv.item.ItemDrag
import com.drake.brv.item.ItemSwipe
import com.drake.brv.utils.addModels
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.models
import com.drake.brv.utils.mutable
import com.drake.brv.utils.setDifferModels
import com.drake.brv.utils.setup
import com.drake.engine.base.EngineFragment
import com.drake.engine.utils.FileUtils
import com.drake.engine.utils.PinyinUtils
import com.drake.net.utils.scope
import com.drake.net.utils.scopeDialog
import com.drake.net.utils.withIO
import com.drake.tooltip.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanzou.cloud.R
import com.lanzou.cloud.base.BaseEditDialog
import com.lanzou.cloud.databinding.FragmentFileBaseBinding
import com.lanzou.cloud.enums.FileSortField
import com.lanzou.cloud.enums.FileSortRule
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.event.Backable
import com.lanzou.cloud.event.FileAction
import com.lanzou.cloud.event.OnFileNavigateListener
import com.lanzou.cloud.event.OnLayoutChangeListener
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.model.FilterSortModel
import com.lanzou.cloud.network.Repository
import com.lanzou.cloud.ui.activity.WebActivity
import com.lanzou.cloud.utils.FileJavaUtils
import com.lanzou.cloud.utils.VibrationManager
import com.lanzou.cloud.utils.addModel
import com.lanzou.cloud.utils.removeModel
import com.lanzou.cloud.utils.removeModels
import com.lanzou.cloud.utils.updateModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.Collections

abstract class FileFragment(private val layoutPosition: LayoutPosition = LayoutPosition.LEFT) :
  EngineFragment<FragmentFileBaseBinding>(R.layout.fragment_file_base), OnLayoutChangeListener,
  Backable, FileAction, MenuProvider {

  protected val viewModel by viewModels<HomeViewModel>(ownerProducer = { requireParentFragment() })

  protected val mData = mutableListOf<FileInfoModel>()

  @Suppress("UNCHECKED_CAST")
  protected val models get() = binding.fileRv.models as List<FileInfoModel>

  abstract suspend fun getData(page: Int): List<FileInfoModel>?

  private var isFirst = true

  override fun initData() {
    lifecycleScope.launch {
      viewModel.currentPositionFlow.collect {
        onLayoutChange(it)
      }
    }

    lifecycleScope.launch {
      viewModel.filterSortModel.collect {
        if (binding.fileRv.models == null) {
          return@collect
        }
        if (viewModel.focusedPositionFlow.value != layoutPosition) {
          return@collect
        }
        sort(it)
      }
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun initView() {
    binding.m = viewModel
    binding.lifecycleOwner = this

    binding.fileRv.setup {
      setAnimation(AnimationType.SLIDE_RIGHT)
      setCheckableType(R.layout.item_list_fileinfo)
      addType<FileInfoModel>(R.layout.item_list_fileinfo)
      addType<String>(R.layout.item_file_parent)

      onChecked { position, checked, allChecked ->
        val model = getModel<FileInfoModel>(position)
        model.isChecked = checked
      }

      onToggle { position, toggleMode, end ->
        // val model = getModelOrNull<FileInfoModel>(position) ?: return@onToggle
        // model.isCheckable = toggleMode
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
        if (onItemLongClick(getModel(), modelPosition)) {
          return@onLongClick
        }
        if (!toggleMode) {
          toggle()
          setChecked(layoutPosition, true)
        }
      }

      var minPosition : Int = 0;
      var maxPosition : Int = 0;

      R.id.item.onFastClick {
        if (toggleMode) {
          minPosition = 0;
          maxPosition = 0;
          val model = getModel<FileInfoModel>()
          setChecked(layoutPosition, !model.isChecked)
          return@onFastClick
        }
        viewModel.focusPosition(this@FileFragment.layoutPosition)
        onItemClick(getModel(), modelPosition)
      }

      R.id.header.onFastClick {
        if (toggleMode) {
          return@onFastClick
        }
        onBack()
      }



      var swipeModel = false;
      itemTouchHelper = ItemTouchHelper(object : MyItemTouchCallback() {
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
          VibrationManager.get().vibrateOneShot(50)
          var targetPosition = viewHolder.layoutPosition;
          if(!swipeModel) {
            minPosition = targetPosition;
            maxPosition = targetPosition;
            swipeModel = true;
          } else {
            if(minPosition > targetPosition) {
              minPosition = targetPosition;
            }
            if(targetPosition > maxPosition) {
              maxPosition = targetPosition;
            }
            swipeModel = false;
          }
          for(i in minPosition..maxPosition) {
            setChecked(i, true)
          }
          toggleMulti(true)
        }
      })
    }

    binding.refresh.onRefresh {
      scope {
        val data = onSort(withIO {
          getData(index)
        }, viewModel.filterSortModel.value)

        if (index == 1) {
          mData.clear()
        }

        val showBackItem = showBackItem()
        if (showBackItem()) {
          binding.fileRv.bindingAdapter.run {
            removeHeaderAt(0)
            addHeader("...")
          }
          // FIXME: 防止 mData 与实际索引不匹配的问题
          // mData.add(FileInfoModel(name = "..."))
        }

        data?.let {
          mData.addAll(it)
        }

        addData(data, isEmpty = { data.isNullOrEmpty() && !showBackItem }) {
          isLoadMore(data)
        }
        onLoadEnd(data)
      }
    }

    binding.refresh.stateLayout?.onContent {
      animate().setDuration(0).alpha(0F).withEndAction {
        animate().setDuration(500).alpha(1F)
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

  protected open fun onItemLongClick(model: FileInfoModel, position: Int): Boolean {
    return false
  }

  protected open fun showBackItem(): Boolean {
    return false
  }

  /**
   * 触发返回事件，当返回 true 时，按下返回键将执行 finish，false 则啥也不做
   */
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

  protected open fun onSort(
    data: List<FileInfoModel>?,
    filterSortModel: FilterSortModel,
  ): List<FileInfoModel>? {

    val rule = filterSortModel.rule
    val field = filterSortModel.field

    return data?.sortedWith { o1, o2 ->
      if (o1.isDirectory && o2.isFile) {
        -1
      } else if (o1.isFile && o2.isDirectory) {
        1
      } else {
        when (rule) {
          FileSortRule.ASC -> when (field) {
            FileSortField.NAME -> PinyinUtils.ccs2Pinyin(o1.name)
              .compareTo(PinyinUtils.ccs2Pinyin(o2.name))

            FileSortField.TIME -> o1.updateTime.compareTo(o2.updateTime)
            FileSortField.SIZE -> o1.length.compareTo(o2.length)
          }

          FileSortRule.DESC -> when (field) {
            FileSortField.NAME ->
              PinyinUtils.ccs2Pinyin(o2.name)
                .compareTo(PinyinUtils.ccs2Pinyin(o1.name))

            FileSortField.TIME -> o2.updateTime.compareTo(o1.updateTime)
            FileSortField.SIZE -> o2.length.compareTo(o1.length)
          }
        }
      }
    }
  }

  override fun onSearch(keyWorld: String?) {
    if (keyWorld.isNullOrBlank()) {
      binding.fileRv.models = mData
    } else {
      binding.fileRv.setDifferModels(mData.filter {
        PinyinUtils.ccs2Pinyin(it.name).lowercase()
          .contains(PinyinUtils.ccs2Pinyin(keyWorld).lowercase())
      })
    }
  }

  override fun onMkdir(name: String, path: String) {
    val fileRv = binding.fileRv
    val position = getInsertPosition(name)
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
    if (!isMultiMode) {
      binding.fileRv.bindingAdapter.checkedPosition.clear()
    }
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
    addFile(getInsertPosition(fileInfoModel.name), fileInfoModel)
  }

  override fun addFile(position: Int, fileInfoModel: FileInfoModel) {
    val newFile = fileInfoModel.copy()
    newFile.highlight = true
    mData.add(position, newFile)
    binding.fileRv.run {
      addModel(position, newFile)
      scrollToPosition(position)
    }
  }

  override fun addFiles(files: List<FileInfoModel>) {
    if (files.isEmpty()) {
      return
    }
    val position = getInsertPosition(files[0].name)
    val newFiles = files.map { it.copy(highlight = true) }
    binding.fileRv.addModels(newFiles, true, position)
    mData.addAll(position, newFiles)
    binding.fileRv.post {
      scrollToPosition(position)
    }
  }

  override fun scrollToPosition(position: Int) {
    val layoutManager = binding.fileRv.layoutManager
    // FIXME: 当切换layoutManager 后可能出现问题
    when (layoutManager) {
      is LinearLayoutManager -> layoutManager.scrollToPositionWithOffset(position, 0)
      is StaggeredGridLayoutManager -> layoutManager.scrollToPositionWithOffset(position, 0)
    }
  }

  /**
   * 获取准确的插入位置
   */
  @Suppress("unchecked_cast")
  protected open fun getInsertPosition(name: String? = null): Int {
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

  override fun getFile(path: String): FileInfoModel? {
    return mData.find { it.path == path }
  }

  override fun deleteFile(position: Int, file: FileInfoModel) {
    if (file.path.isEmpty()) {
      return
    }
    MaterialAlertDialogBuilder(requireContext())
      .setTitle("删除文件")
      .setMessage("确认要删除文件: ${file.name}")
      .setPositiveButton("确认") { dialog, _ ->
        scopeDialog {
          File(file.path).deleteRecursively()
          mData.removeAt(binding.fileRv.removeModel(position))
        }.finally {
          toggleMulti(false)
        }
      }
      .setNegativeButton("取消", null)
      .show()
  }

  override fun deleteFiles(positions: List<Int>, files: List<FileInfoModel>) {
    scopeDialog {
      files.forEach {
        if (it.path.isNotEmpty()) {
          File(it.path).deleteRecursively()
        }
      }
      binding.fileRv.removeModels(positions) {
        mData.removeAt(it)
      }
    }.finally {
      toggleMulti(false)
    }
  }

  override fun renameFile(position: Int, file: FileInfoModel) {
    if (file.path.isEmpty()) {
      return
    }
    BaseEditDialog(requireContext()).apply {
      setTitle("重命名")
      editValue = file.name
      hint = "新文件名"
      setPositiveButton("修改") { dialog, _ ->
        if (editValue.isEmpty()) {
          toast("请输入文件名称")
          return@setPositiveButton
        }
        file.name = editValue
        val targetFile = File(file.path)
        val renamedFile = File(targetFile.parentFile, editValue)
        if (targetFile.renameTo(renamedFile)) {
          file.path = renamedFile.path
        }
        binding.fileRv.updateModel(position)
        val targetPosition = getInsertPosition(file.name)
        if (targetPosition != position) {
          Collections.swap(binding.fileRv.mutable, position, targetPosition)
          binding.fileRv.bindingAdapter.let {
            it.notifyItemMoved(position + it.headerCount, targetPosition + it.headerCount)
          }
          scrollToPosition(targetPosition)
        }
      }
      setNegativeButton("取消", null)
      show()
    }
  }

  override fun showDetail(position: Int, file: FileInfoModel) {
    val items = arrayOf(
      "文件名称: ${file.name}",
      "目录: ${file.path}",
      "类型: ${file.extension}",
      "大小: ${file.size}",
      "修改时间: ${file.updateTimeStr}"
    )
    MaterialAlertDialogBuilder(requireContext())
      .setTitle("文件详情")
      .setItems(items, null)
      .setPositiveButton("关闭", null)
      .setNegativeButton("打开文件") { dialog, _ ->
        FileJavaUtils.openFile(file.path)
      }
      .setNeutralButton("MD5") { dialog, _ ->
        scopeDialog {
          val md5 = coroutineScope {
            FileUtils.getFileMD5ToString(file.path)
          }
          MaterialAlertDialogBuilder(requireContext())
            .setTitle("文件MD5")
            .setMessage(md5)
            .setPositiveButton("关闭", null)
            .show()
        }
      }
      .show()
  }

  override fun sort(filterSortModel: FilterSortModel) {
    scope {
      val sortedData = coroutineScope {
        onSort(mData, filterSortModel)
      }
      binding.fileRv.models = sortedData
      mData.clear()
      sortedData?.let {
        mData.addAll(it)
      }
    }
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    when (menuItem.itemId) {
      R.id.delete -> {
        val bindingAdapter = binding.fileRv.bindingAdapter
        val checkedCount = bindingAdapter.checkedCount
        if (checkedCount == 0) {
          toast("没有选择文件")
          return true
        }
        MaterialAlertDialogBuilder(requireContext())
          .setTitle("删除文件")
          .setMessage("是否删除已选择的${checkedCount}个文件")
          .setPositiveButton("删除") { dialog, _ ->
            deleteFiles(bindingAdapter.checkedPosition, bindingAdapter.getCheckedModels())
          }
          .setNegativeButton("取消", null)
          .show()

        return true
      }

      else -> {
        menuItem.isChecked = true
        when (menuItem.itemId) {
          R.id.sort_name -> viewModel.sortField(FileSortField.NAME)
          R.id.sort_time -> viewModel.sortField(FileSortField.TIME)
          R.id.sort_size -> viewModel.sortField(FileSortField.SIZE)
          R.id.sort_asc -> viewModel.sortRule(FileSortRule.ASC)
          R.id.sort_desc -> viewModel.sortRule(FileSortRule.DESC)
        }
      }
    }
    return false
  }
}


open class MyItemTouchCallback : ItemTouchHelper.Callback() {

  /** 侧滑到底item消失时 */
  override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
    /*val adapter = viewHolder.bindingAdapter as? BindingAdapter ?: return
    val layoutPosition = viewHolder.layoutPosition
    val headerCount = adapter.headerCount
    if (layoutPosition < headerCount) {
      adapter.removeHeader(layoutPosition, true)
    } else {
      val models = adapter.models as? MutableList
      if (models != null) {
        models.removeAt(layoutPosition - headerCount)
        adapter.notifyItemRemoved(layoutPosition)
      }
    }*/

  }

  /**
   * 返回值表示拖拽/侧滑的方向
   * @param viewHolder 拖拽触发的Item
   */
  override fun getMovementFlags(
    recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
  ): Int {
    var drag = 0
    var swipe = 0
    if (viewHolder is BindingViewHolder) {
      val model = viewHolder.getModel<Any>()
      if (model is ItemDrag) drag = model.itemOrientationDrag
      if (model is ItemSwipe) swipe = model.itemOrientationSwipe
    }
    return makeMovementFlags(drag, swipe)
  }

  /** 绘制拖拽或者侧滑动画 */
  override fun onChildDraw(
    c: Canvas,
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder,
    dX: Float,
    dY: Float,
    actionState: Int,
    isCurrentlyActive: Boolean,
  ) {
    /*if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
      val swipeView = viewHolder.itemView.findViewWithTag<View>("swipe")
      if (swipeView != null) {
        swipeView.translationX = dX
      } else {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
      }
    } else {
      super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }*/
  }

  /**
   * 滑动距离速率来判断当前是否执行滑动删除事件(可以理解为移出itemView)
   * @param viewHolder 拖拽触发的Item
   */
  override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
    return 0f
  }

  override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
    super.clearView(recyclerView, viewHolder)
    val view = viewHolder.itemView.findViewWithTag<View>("swipe")
    if (view != null) {
      view.translationX = 0F
    }
  }

  /**
   * 当拖拽动作完成且松开手指时触发, 如果拖拽起始位置等于目标位置则属于无效移动, 不回调当前方法
   * @param source 触发拖拽的Item
   * @param target 拖拽目标的Item
   */
  open fun onDrag(source: BindingViewHolder, target: BindingViewHolder) {

  }

  private var lastActionState: Int = 0
  private var sourceViewHolder: BindingViewHolder? = null
  private var targetViewHolder: BindingViewHolder? = null

  /**
   * 拖拽或者侧滑导致的状态变化
   * @param vh 当前触发的Item
   * @param actionState 触发的状态
   * @see ItemTouchHelper.ACTION_STATE_DRAG 拖拽
   * @see ItemTouchHelper.ACTION_STATE_SWIPE 侧滑
   * @see ItemTouchHelper.ACTION_STATE_IDLE 闲置
   */
  override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, actionState: Int) {
    when (actionState) {
      ItemTouchHelper.ACTION_STATE_IDLE -> {
        val source = sourceViewHolder
        val target = targetViewHolder
        if (lastActionState == ItemTouchHelper.ACTION_STATE_DRAG &&
          source is BindingViewHolder &&
          target is BindingViewHolder &&
          startMovingPosition != null &&
          startMovingPosition != source.bindingAdapterPosition
        ) {
          onDrag(source, target)
        }
        // 手指放下, 重置起始移动位置
        startMovingPosition = null
      }

      else -> {
        this.lastActionState = actionState
      }
    }
  }

  /**记录开始移动位置, 如果拖拽起始位置等于目标位置则属于无效移动*/
  private var startMovingPosition: Int? = null

  /**
   * 拖拽移动超过其他item时, 其返回值表示是否已经拖拽替换(会触发函数onMoved)
   * @return 返回false 禁止被拖拽交换移动
   */
  override fun onMove(
    recyclerView: RecyclerView,
    source: RecyclerView.ViewHolder,
    target: RecyclerView.ViewHolder,
  ): Boolean {
    /*val adapter = recyclerView.bindingAdapter as? BindingAdapter ?: return false
    val currentPosition = recyclerView.getChildLayoutPosition(source.itemView)
    val targetPosition = recyclerView.getChildLayoutPosition(target.itemView)

    val models = adapter.models as? MutableList
    if (models != null && source is BindingViewHolder && target is BindingViewHolder && adapter.isModel(targetPosition)) {
      val fromPosition = currentPosition - adapter.headerCount
      val toPosition = targetPosition - adapter.headerCount
      val fromItem = models[fromPosition]
      models.removeAt(fromPosition)
      models.add(toPosition, fromItem)
      adapter.notifyItemMoved(currentPosition, targetPosition)
      // 记录起始移动位置
      if (startMovingPosition == null) {
        startMovingPosition = target.bindingAdapterPosition
      }
      sourceViewHolder = source
      targetViewHolder = target
      return true
    }
    return false*/
    return true;
  }
}