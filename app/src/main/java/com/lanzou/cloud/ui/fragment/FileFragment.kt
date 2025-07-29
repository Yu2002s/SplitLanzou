package com.lanzou.cloud.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.drake.brv.BindingAdapter
import com.drake.brv.BindingAdapter.BindingViewHolder
import com.drake.brv.annotaion.AnimationType
import com.drake.brv.annotaion.ItemOrientation
import com.drake.brv.item.ItemDrag
import com.drake.brv.item.ItemSwipe
import com.drake.brv.listener.DefaultItemTouchCallback
import com.drake.brv.listener.ItemDifferCallback
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
import com.lanzou.cloud.enums.FilePageType
import com.lanzou.cloud.enums.FileSortField
import com.lanzou.cloud.enums.FileSortRule
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.event.Backable
import com.lanzou.cloud.event.FileAction
import com.lanzou.cloud.event.OnFileNavigateListener
import com.lanzou.cloud.event.OnLayoutChangeListener
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.model.FilePathModel
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

/**
 * 对文件操作的基类
 *
 * @param layoutPosition 布局所在的位置，分为 LEFT、RIGHT
 */
abstract class FileFragment(
  val layoutPosition: LayoutPosition = LayoutPosition.LEFT,
  val filePageType: FilePageType = FilePageType.LOCAL,
) :
  EngineFragment<FragmentFileBaseBinding>(R.layout.fragment_file_base), OnLayoutChangeListener,
  Backable, FileAction, MenuProvider {

  companion object {

    private const val TAG = "FileFragment"

  }

  protected val viewModel by viewModels<HomeViewModel>(ownerProducer = { requireParentFragment() })

  /**
   * 快照数据，主要用于搜索过滤使用
   */
  protected val mData = mutableListOf<FileInfoModel>()

  /**
   * 实际展示在页面的数据
   */
  @Suppress("UNCHECKED_CAST")
  protected val models get() = binding.fileRv.models as List<FileInfoModel>

  /**
   * 路径列表
   */
  protected val paths = mutableListOf<FilePathModel>()

  protected val currentPage get() = binding.refresh.index

  /**
   * 获取所需的数据
   *
   * @param page 当前页码
   * @return 返回文件集合
   */
  abstract suspend fun getData(path: String?, page: Int): List<FileInfoModel>?

  /**
   * 是否第一次可见
   */
  private var isFirst = true

  /**
   * 初始化数据
   */
  override fun initData() {
    paths.clear()
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.RESUMED) {
        viewModel.currentPositionFlow.collect {
          // 监听布局改变，如果发生改变，则更改 RV 的 layoutManager
          onLayoutChange(it)
        }
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

    var minPosition = 0
    var maxPosition = 0
    var swipeModel = false
    binding.fileRv.setup {
      setAnimation(AnimationType.SLIDE_RIGHT) // 指定动画
      setCheckableType(R.layout.item_list_fileinfo) // 返回项不需要多选
      addType<FileInfoModel>(R.layout.item_list_fileinfo) // 文件布局
      // addType<String>(R.layout.item_file_parent) // 返回布局

      onChecked { position, checked, allChecked ->
        val model = getModel<FileInfoModel>(position)
        model.isChecked = checked
      }

      // 监听多选事件
      onToggle { position, toggleMode, end ->
        // val model = getModelOrNull<FileInfoModel>(position) ?: return@onToggle
        // model.isCheckable = toggleMode
        if (this@FileFragment.layoutPosition == LayoutPosition.LEFT) {
          viewModel.toggleLeft(toggleMode) // 左侧多选
        } else {
          viewModel.toggleRight(toggleMode) // 右侧多选
        }
        if (!toggleMode) {
          checkedAll(false)
          minPosition = 0
          maxPosition = 0
          swipeModel = false
        }
      }

      R.id.item.onLongClick {
        // 如果支持长按事件，则不进行默认的多选操作
        if (onItemLongClick(getModel(), modelPosition)) {
          return@onLongClick
        }
        if (!toggleMode) {
          // 切换到多选
          toggle()
          setChecked(modelPosition, true)
        }
      }


      R.id.item.onFastClick {
        val model = getModel<FileInfoModel>()
        // 聚焦当前布局位置
        viewModel.focusPosition(this@FileFragment.layoutPosition)
        if (model.isDotDot) {
          if (!toggleMode)
            onNavigateUp()
          return@onFastClick
        }

        // 点击事件监听
        if (toggleMode) {
          if(swipeModel) {
            minPosition = 0
            maxPosition = 0
            swipeModel = false
            Log.d("滑动触发多选", "首次滑动后单选，取消滑动多选功能")
          }
          setChecked(modelPosition, !model.isChecked)
          return@onFastClick
        }
        // 执行默认的点击
        onItemClick(model, modelPosition)
      }

      itemTouchHelper = ItemTouchHelper(object : DefaultItemTouchCallback() {
        override fun onChildDraw(
          c: Canvas,
          recyclerView: RecyclerView,
          viewHolder: RecyclerView.ViewHolder,
          dX: Float,
          dY: Float,
          actionState: Int,
          isCurrentlyActive: Boolean,
        ) {
          //Log.d("滑动触发多选", "onChildDraw调用")
        }
        /*override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
          return 0f
        }
        override fun onMove(
          recyclerView: RecyclerView,
          source: RecyclerView.ViewHolder,
          target: RecyclerView.ViewHolder,
        ): Boolean {
          return false
        }*/
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
          //Log.d("滑动触发多选", direction.toString())
          VibrationManager.get().vibrateOneShot(50)
          val targetPosition = viewHolder.layoutPosition
          if (!swipeModel) {
            minPosition = targetPosition
            maxPosition = targetPosition
            Log.d("滑动触发多选", "滑动开始, targetPosition = " + targetPosition)
            swipeModel = true
          } else {
            if (minPosition >= targetPosition) {
              minPosition = targetPosition
            }
            if (targetPosition > maxPosition) {
              maxPosition = targetPosition
            }
            swipeModel = false
            Log.d("滑动触发多选", "滑动结束, targetPositon = " + targetPosition + ", minPosition = " + minPosition + ", maxPosition = " + maxPosition)
          }
          for (i in minPosition..maxPosition) {
            setChecked(i, true)
          }
          binding.fileRv.bindingAdapter.toggle(true)
          binding.fileRv.bindingAdapter.notifyItemChanged(minPosition, maxPosition - minPosition)
        }
      })
    }

    binding.refresh.onRefresh {
      scope {
        val data = onSort(withIO {
          getData(getCurrentPath(), index)
        }, viewModel.filterSortModel.value)?.toMutableList()

        if (index == 1) {
          mData.clear()
        }

        // 根据子类判断是否显示返回
        val showBackItem = hasParentDirectory() && index == 1

        data?.let {
          if (showBackItem) {
            it.add(0, FileInfoModel(name = ".."))
          }
          mData.addAll(it)
        }

        val page = index

        addData(data, isEmpty = { data.isNullOrEmpty() && !showBackItem }) {
          isLoadMore(data)
        }
        onLoadEnd(data, page)
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

  fun refreshPathSubTitle() {
    val pageType = when (filePageType) {
      FilePageType.LOCAL -> "本地"
      FilePageType.REMOTE -> "远程"
    }
    (activity as? AppCompatActivity)?.supportActionBar?.subtitle = pageType + " - " + getFullPath()
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
      val fileRv = binding.fileRv
      val layoutManager = when {
        positon == LayoutPosition.MIDDLE -> LinearLayoutManager(requireContext())
        positon != layoutPosition -> StaggeredGridLayoutManager(
          2,
          StaggeredGridLayoutManager.VERTICAL
        )

        else -> LinearLayoutManager(requireContext())
      }
      if (fileRv.layoutManager?.javaClass != layoutManager.javaClass) {
        fileRv.layoutManager = layoutManager
      }
    }
  }

  /**
   * Item 项点击事件监听
   *
   * @param model 对应的文件数据
   * @param position 点击的位置(去掉 header 的位置)
   */
  protected open fun onItemClick(model: FileInfoModel, position: Int) {
    if (model.isFile) {
      // 请求上传文件
      navigateTo(model, position)
      return
    }
    if (paths.isEmpty()) {
      return
    }
    paths[paths.lastIndex].scrollPosition = getFirstVisiblePosition()
    val path = model.path.ifEmpty { model.folderId }
    paths.add(FilePathModel(path = path, name = model.name))
    refreshPathSubTitle()
    binding.refresh.showLoading()
  }

  /**
   * Item 项长按事件监听
   */
  protected open fun onItemLongClick(model: FileInfoModel, position: Int): Boolean {
    return false
  }

  /**
   * 是否顶部显示返回项
   *
   * @return true 显示 false 不显示
   */
  protected open fun hasParentDirectory(): Boolean {
    return false
  }

  /**
   * 触发返回事件，当返回 true 时，按下返回键将执行 finish，false 则啥也不做
   */
  override fun onNavigateUp(): Boolean {
    if (paths.size <= 1) {
      return true
    }
    paths.removeAt(paths.lastIndex)
    refreshPathSubTitle()
    refresh()
    return false
  }

  /**
   * 向上执行导航事件
   */
  protected fun navigateTo(fileInfoModel: FileInfoModel, position: Int) {
    val parentFragment = parentFragment ?: return
    if (parentFragment is OnFileNavigateListener) {
      parentFragment.navigate(fileInfoModel, position, filePageType)
    }
  }

  /**
   * 加载结束事件
   *
   * @param data 已加载的数据
   */
  protected open fun onLoadEnd(data: List<FileInfoModel>?, page: Int) {
    if (paths.isEmpty() || page != 1) {
      return
    }
    val firstVisiblePosition = getFirstVisiblePosition()
    val scrollPosition = paths.last().scrollPosition
    if (firstVisiblePosition != scrollPosition) {
      scrollToPosition(scrollPosition)
    }
  }

  /**
   * 定义对数据进行排序的规则
   *
   * @param data 数据集合
   * @param filterSortModel 排序规则
   * @return 返回过滤后的数据
   */
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
    val file = FileInfoModel(name = name, folderId = path)
    val fileRv = binding.fileRv
    val position = getInsertPosition(name)
    fileRv.mutable.add(position, file)
    mData.add(position, file)
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

  /**
   * 是否还能加载更多数据
   *
   * @param data 当前数据集合
   * @return true 可以，false 不可以
   */
  protected open fun isLoadMore(data: List<FileInfoModel>?): Boolean {
    return false
  }

  /**
   * 获取当前的路径
   */
  open fun getCurrentPath(): String? {
    return paths.lastOrNull()?.path
  }

  open fun getFullPath() = paths.joinToString("/") { it.name }

  override fun getCheckedFiles(ignoreDirectory: Boolean): List<FileInfoModel> {
    return models.filter { it.isChecked && (!ignoreDirectory || it.isFile) }
  }

  override fun getCheckedPositions(): List<Int> {
    return binding.fileRv.bindingAdapter.checkedPosition
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
    binding.refresh.showLoading()
  }

  override fun getFile(path: String): FileInfoModel? {
    return mData.find { it.path == path }
  }

  override fun removeFile(position: Int, file: FileInfoModel) {
    if (position > 0) {
      mData.removeAt(binding.fileRv.removeModel(position))
    } else {
      // 处理 position 小于 0 的情况
      val position = mData.indexOf(file)
      mData.removeAt(binding.fileRv.removeModel(position))
    }
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
          withIO {
            File(file.path).deleteRecursively()
          }
          removeFile(position, file)
        }.finally {
          toggleMulti(false)
        }
      }
      .setNegativeButton("取消", null)
      .show()
  }

  override fun deleteFiles(positions: List<Int>, files: List<FileInfoModel>) {
    scopeDialog {
      withIO {
        files.forEach {
          if (it.path.isNotEmpty()) {
            File(it.path).deleteRecursively()
          }
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
          binding.fileRv.bindingAdapter.notifyItemMoved(position, targetPosition)
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

  override suspend fun moveFile(
    position: Int,
    current: FileInfoModel,
    targetPath: String?
  ): FileInfoModel? {
    targetPath ?: return null
    // FIXME: 不判断存不存在，暂时直接替换文件
    val targetFilePath = targetPath + File.separator + current.name
    if (current.path == targetFilePath) {
      return null
    }
    val result = if (current.isDirectory) {
      FileUtils.moveDir(current.path, targetFilePath) { true }
    } else {
      FileUtils.moveFile(current.path, targetFilePath) { true }
    }

    if (result) {
      return current.copy(path = targetFilePath)
    }
    return null
  }

  override fun shareFile(position: Int, file: FileInfoModel) {
    if (!FileUtils.isFile(file.path)) {
      toast("文件可能未下载完成，请刷新检查后重试")
      return
    }
    com.lanzou.cloud.utils.FileUtils.shareFile(requireContext(), file.path)
  }

  override fun copyFile(
    position: Int,
    current: FileInfoModel,
    targetPath: String?
  ): FileInfoModel? {
    targetPath ?: return null
    val targetFilePath = targetPath + File.separator + current.name
    if (current.path == targetFilePath) {
      return null
    }
    try {
      val file = File(current.path)
      val target = File(targetFilePath)
      file.copyRecursively(target, true)
      return current.copy(path = targetFilePath)
    } catch (_: Exception) {
      return null
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

  private fun getFirstVisiblePosition(): Int {
    val layoutManager = binding.fileRv.layoutManager
    return when (layoutManager) {
      is LinearLayoutManager -> layoutManager.findFirstCompletelyVisibleItemPosition()
      is StaggeredGridLayoutManager -> {
        val arr = layoutManager.findFirstCompletelyVisibleItemPositions(null)
        arr[0]
      }

      else -> 0
    }
  }
}