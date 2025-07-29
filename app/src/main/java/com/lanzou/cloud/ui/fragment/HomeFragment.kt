package com.lanzou.cloud.ui.fragment

import android.animation.ObjectAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.drake.engine.base.EngineNavFragment
import com.drake.engine.utils.dp
import com.drake.net.utils.scope
import com.drake.net.utils.scopeDialog
import com.drake.net.utils.withIO
import com.drake.tooltip.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.lanzou.cloud.LanzouApplication
import com.lanzou.cloud.R
import com.lanzou.cloud.data.Upload
import com.lanzou.cloud.databinding.FragmentHomeBinding
import com.lanzou.cloud.enums.FilePageType
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.event.OnFileNavigateListener
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.model.LocalPathModel
import com.lanzou.cloud.model.PathModel
import com.lanzou.cloud.model.RemotePathModel
import com.lanzou.cloud.service.DownloadService
import com.lanzou.cloud.service.UploadService
import com.lanzou.cloud.ui.dialog.FileActionDialog
import com.lanzou.cloud.ui.dialog.FileMkdirDialog
import com.lanzou.cloud.ui.dialog.FileSearchDialog
import com.lanzou.cloud.utils.getWindowWidth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class HomeFragment : EngineNavFragment<FragmentHomeBinding>(R.layout.fragment_home),
  OnFileNavigateListener, ServiceConnection, MenuProvider {

  companion object {

    private const val TAG = "HomeFragment"

  }

  private val leftPaths = mutableListOf(
    RemotePathModel(
      name = "远程",
      fragment = LanzouFileFragment.newInstance(name = "根目录")
    ),
    LocalPathModel(
      name = "本地",
      fragment = UploadFileSelectorFragment.newInstance(position = LayoutPosition.LEFT)
    )
  )

  private val rightPaths = mutableListOf(
    RemotePathModel(
      name = "远程",
      fragment = LanzouFileFragment.newInstance(position = LayoutPosition.RIGHT, name = "根目录")
    ),
    LocalPathModel(
      name = "本地",
      fragment = UploadFileSelectorFragment.newInstance()
    ),
    LocalPathModel(name = "软件", fragment = UploadAppSelectorFragment()),
  )

  private val windowWidth = getWindowWidth()

  private val leftLp by lazy {
    binding.leftContent.layoutParams as LinearLayout.LayoutParams
  }

  private val rightLp by lazy {
    binding.rightContent.layoutParams as LinearLayout.LayoutParams
  }

  private val homeViewModel by viewModels<HomeViewModel>()

  private val currentPaths
    get() = when (currentFocusedPosition) {
      LayoutPosition.LEFT -> leftPaths
      LayoutPosition.RIGHT -> rightPaths
      else -> throw IllegalStateException()
    }

  private val currentPathModel get() = currentPaths[currentVp.currentItem]

  private val currentFilePageType get() = currentFileFragment.filePageType

  private val targetFilePageType
    get() = when (currentFocusedPosition) {
      LayoutPosition.LEFT -> currentRightFileFragment.filePageType
      LayoutPosition.RIGHT -> currentLeftFileFragment.filePageType
      else -> throw IllegalStateException()
    }

  private val currentFileFragment
    get() = when (currentFocusedPosition) {
      LayoutPosition.LEFT -> currentLeftFileFragment
      LayoutPosition.RIGHT -> currentRightFileFragment
      else -> throw IllegalStateException()
    }

  private val currentLeftFileFragment get() = leftPaths[binding.vpLeft.currentItem].fragment

  private val currentRightFileFragment get() = rightPaths[binding.vpRight.currentItem].fragment

  private val currentFocusedPosition get() = homeViewModel.focusedPositionFlow.value

  private val currentVp
    get() = when (currentFocusedPosition) {
      LayoutPosition.LEFT -> binding.vpLeft
      LayoutPosition.RIGHT -> binding.vpRight
      else -> throw IllegalStateException()
    }

  private val currentVpAdapter
    get() = (when (currentFocusedPosition) {
      LayoutPosition.LEFT -> binding.vpLeft
      LayoutPosition.RIGHT -> binding.vpRight
      else -> throw IllegalStateException()
    }).adapter as FilePagerAdapter

  private val currentLeftVpAdapter get() = binding.vpLeft.adapter as FilePagerAdapter

  private val currentRightVpAdapter get() = binding.vpRight.adapter as FilePagerAdapter

  /**
   * 目标 fragment
   */
  private val targetFileFragment: FileFragment
    get() {
      if (currentFocusedPosition == LayoutPosition.LEFT) {
        return currentRightFileFragment
      }
      return currentLeftFileFragment
    }

  /**
   * 目标路径列表
   */
  private val targetPaths: List<PathModel>
    get() {
      if (currentFocusedPosition == LayoutPosition.LEFT) {
        return rightPaths
      }
      return leftPaths
    }

  private val targetPathModel: PathModel
    get() {
      if (currentFocusedPosition == LayoutPosition.LEFT) {
        return rightPaths[binding.vpRight.currentItem]
      }
      return leftPaths[binding.vpLeft.currentItem]
    }

  private val targetPosition
    get() = when (currentFocusedPosition) {
      LayoutPosition.LEFT -> LayoutPosition.RIGHT
      LayoutPosition.RIGHT -> LayoutPosition.LEFT
      else -> throw IllegalStateException()
    }

  private inner class FilePageChangeCallback(private val paths: List<PathModel>) :
    ViewPager2.OnPageChangeCallback() {
    override fun onPageSelected(position: Int) {
      homeViewModel.focusPosition(paths[position].fragment.layoutPosition)
      requireActivity().invalidateMenu()
      currentFileFragment.refreshPathSubTitle()
    }
  }

  private inner class FileTabSelectedListener(private val position: LayoutPosition) :
    TabLayout.OnTabSelectedListener {
    override fun onTabReselected(tab: TabLayout.Tab) {
      val popupMenu = PopupMenu(requireContext(), tab.view)
      val menu = popupMenu.menu
      menu.add(0, 0, 0, "关闭")
      // menu.add(1, 1, 1, "复制")
      // menu.add(2, 2, 2,"关闭全部")
      popupMenu.show()
      popupMenu.setOnMenuItemClickListener {
        when (it.itemId) {
          0 -> removeFilePage(tab.position, position)
          1 -> {
            addFilePage(
              tab.text.toString(),
              targetFilePageType,
              currentPathModel.path!!,
              targetPosition,
            )
          }
        }
        true
      }
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
    }
  }

  private lateinit var onBackPressedCallback: OnBackPressedCallback

  private lateinit var downloadService: DownloadService
  private lateinit var uploadService: UploadService

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requireContext().bindService(
      Intent(
        requireContext(), DownloadService::class.java
      ), this,
      Context.BIND_AUTO_CREATE
    )
    requireContext().bindService(
      Intent(
        requireContext(), UploadService::class.java
      ), this,
      Context.BIND_AUTO_CREATE
    )

    // 清理临时文件
    scope(Dispatchers.IO) {
      val file = File(LanzouApplication.tempPath)
      if (file.exists()) {
        file.deleteRecursively()
      }
    }
  }

  override fun initData() {

    lifecycleScope.launch {
      homeViewModel.currentPositionFlow.collect {
        animChangeContent(it)
      }
    }

    lifecycleScope.launch {
      homeViewModel.leftMultiModeFlow.collect {
        it ?: return@collect
        homeViewModel.focusPosition(LayoutPosition.LEFT)
        currentFileFragment.toggleMulti(it)
      }
    }

    lifecycleScope.launch {
      homeViewModel.rightMultiModeFlow.collect {
        it ?: return@collect
        homeViewModel.focusPosition(LayoutPosition.RIGHT)
        currentFileFragment.toggleMulti(it)
      }
    }

    lifecycleScope.launch {
      homeViewModel.focusedPositionFlow.collect {
        requireActivity().invalidateMenu()
        currentFileFragment.refreshPathSubTitle()
      }
    }
  }

  override fun initView() {
    binding.lifecycleOwner = this
    binding.m = homeViewModel
    binding.btnLeft.setOnClickListener(this)
    binding.btnRight.setOnClickListener(this)
    binding.fabUpload.setOnClickListener(this)
    binding.fabDownload.setOnClickListener(this)
    binding.btnRightSearch.setOnClickListener(this)
    binding.btnLeftSearch.setOnClickListener(this)
    binding.btnLeftMkdir.setOnClickListener(this)
    binding.btnRightMkdir.setOnClickListener(this)
    binding.btnLeftMulti.setOnClickListener(this)
    binding.btnRightMulti.setOnClickListener(this)
    binding.fabUpload.setOnClickListener(this)
    binding.fabDownload.setOnClickListener(this)
    requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

    val contentWidth = (windowWidth - 1.dp) / 2
    leftLp.width = contentWidth
    rightLp.width = contentWidth

    initPage(binding.tabLeft, binding.vpLeft, leftPaths)
    initPage(binding.tabRight, binding.vpRight, rightPaths)

    onBackPressedCallback =
      requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
        if (currentFileFragment.onNavigateUp()) {
          requireActivity().finish()
        }
      }
  }

  private fun initPage(tab: TabLayout, pager: ViewPager2, paths: List<PathModel>) {
    pager.apply {
      offscreenPageLimit = 3
      isUserInputEnabled = false
      adapter = FilePagerAdapter(paths, childFragmentManager, lifecycle)
      registerOnPageChangeCallback(FilePageChangeCallback((paths)))
    }
    TabLayoutMediator(tab, pager) { tab, position ->
      tab.text = paths[position].name
    }.attach()
    val position = if (pager == binding.vpLeft) LayoutPosition.LEFT else LayoutPosition.RIGHT
    tab.addOnTabSelectedListener(FileTabSelectedListener((position)))
  }

  private fun animChangeContent(position: LayoutPosition) {
    if (homeViewModel.beforePosition == position) {
      return
    }
    val vpWidth = (windowWidth - 1.dp) / 2
    val currentLeftWidth = leftLp.width
    val currentRightWidth = rightLp.width
    if (position == LayoutPosition.MIDDLE) {
      val isLeft = homeViewModel.beforePosition == LayoutPosition.LEFT
      animChangeContent(if (isLeft) LayoutPosition.RIGHT else LayoutPosition.LEFT)
      return
    }
    ObjectAnimator.ofInt(vpWidth).apply {
      duration = 400
      start()
      addUpdateListener {
        val value = it.animatedValue as Int
        when (position) {
          LayoutPosition.LEFT -> {
            val letWidth = currentLeftWidth - value
            leftLp.width = if (letWidth <= 1) 1 else letWidth
            rightLp.width = currentRightWidth + value
          }

          LayoutPosition.RIGHT -> {
            val leftWidth = currentLeftWidth + value
            val rightWidth = currentRightWidth - value
            leftLp.width = leftWidth
            rightLp.width = if (rightWidth <= 1) 1 else rightWidth
          }

          else -> throw IllegalStateException()
        }
        binding.leftContent.layoutParams = leftLp
        binding.rightContent.layoutParams = rightLp
      }
    }
  }

  override fun onClick(v: View) {

    when (v.id) {
      R.id.btn_left -> {
        homeViewModel.movePosition()
      }

      R.id.btn_right -> {
        homeViewModel.movePosition(false)
      }

      R.id.btn_left_search -> {
        homeViewModel.focusPosition(LayoutPosition.LEFT)
        showSearchDialog()
      }

      R.id.btn_right_search -> {
        homeViewModel.focusPosition(LayoutPosition.RIGHT)
        showSearchDialog()
      }

      R.id.btn_left_mkdir -> {
        homeViewModel.focusPosition(LayoutPosition.LEFT)
        showMkdirDialog()
      }

      R.id.btn_right_mkdir -> {
        homeViewModel.focusPosition(LayoutPosition.RIGHT)
        showMkdirDialog()
      }

      R.id.btn_left_multi -> homeViewModel.toggleLeft()

      R.id.btn_right_multi -> homeViewModel.toggleRight()

      R.id.fab_upload -> {
        val checkedFiles = currentFileFragment.getCheckedFiles()
        if (checkedFiles.isEmpty()) {
          toast("选中文件为空")
          return
        }
        if (currentFilePageType == targetFilePageType) {
          if (currentFilePageType == FilePageType.LOCAL) {
            moveFiles(currentFileFragment.getCheckedFiles(false))
          } else {
            moveFiles(checkedFiles)
          }
          return
        }
        val currentPath = targetFileFragment.getCurrentPath() ?: return
        val currentFolderName = targetPathModel.name

        MaterialAlertDialogBuilder(requireContext())
          .setTitle("上传")
          .setMessage("已选择${checkedFiles.size}个文件，将上传到: $currentFolderName\n\n【暂时不支持选择文件夹】")
          .setPositiveButton("执行上传") { dialog, _ ->
            uploadFiles(currentPath, checkedFiles, currentFolderName)
          }
          .setNegativeButton("取消", null)
          .show()
      }

      R.id.fab_download -> {
        val checkedFiles = currentFileFragment.getCheckedFiles()
        if (checkedFiles.isEmpty()) {
          toast("选中文件为空")
          return
        }
        if (currentFilePageType == targetFilePageType) {
          if (currentFilePageType == FilePageType.LOCAL) {
            moveFiles(currentFileFragment.getCheckedFiles(false))
          } else {
            moveFiles(checkedFiles)
          }
          return
        }
        val currentPath = targetFileFragment.getCurrentPath()
        if (currentPath == null) {
          toast("不能下载到目标位置")
          return
        }
        MaterialAlertDialogBuilder(requireContext())
          .setTitle("下载")
          .setMessage("已选择${checkedFiles.size}个文件，将下载到: $currentPath\n\n【暂时不支持选择文件夹】")
          .setPositiveButton("执行下载") { dialog, _ ->
            downloadFiles(currentPath, checkedFiles)
          }
          .setNegativeButton("取消", null)
          .show()
      }
    }
  }

  private fun showMkdirDialog() {
    val currentPath = currentFileFragment.getCurrentPath()
    if (currentPath == null) {
      toast("当前路径不能新建")
      return
    }
    FileMkdirDialog(requireContext(), currentPath).apply {
      onConfirm = {
        val isLocalFile = currentFilePageType == FilePageType.LOCAL
        val path: String = if (isLocalFile) {
          currentPath + File.separator + it
        } else {
          currentPath
        }

        currentFileFragment.onMkdir(it!!, path)
      }
      show()
    }
  }

  private fun showSearchDialog() {
    FileSearchDialog(requireContext()).apply {
      onConfirm = {
        currentFileFragment.onSearch(it)
      }
      show()
    }
  }

  override fun onResume() {
    super.onResume()
    onBackPressedCallback.isEnabled = true
  }

  override fun onPause() {
    super.onPause()
    onBackPressedCallback.isEnabled = false
  }

  private fun showTransmissionDialog(
    fileInfoModel: FileInfoModel,
    position: Int,
    filePageType: FilePageType
  ) {
    val currentPath = targetFileFragment.getCurrentPath()
    if (currentPath == null) {
      toast("不能传输到目标路径，请更换路径")
      return
    }
    val isUpload = filePageType == FilePageType.LOCAL
    var message = "是否"
    message += if (isUpload) {
      "上传"
    } else {
      "下载"
    }
    message += fileInfoModel.name

    var currentFolderName = ""
    if (isUpload) {
      currentFolderName = targetPathModel.name
      message += "\n\n上传到: $currentFolderName"
    } else {
      message += "\n\n下载到: $currentPath"
    }

    MaterialAlertDialogBuilder(requireContext())
      .setTitle(if (isUpload) "上传" else "下载")
      .setMessage(message)
      .setPositiveButton("执行") { dialog, _ ->
        if (isUpload) {
          uploadFile(fileInfoModel.path, currentPath, currentFolderName)
        } else {
          downloadFile(currentPath, fileInfoModel)
        }
        targetFileFragment.addFile(fileInfoModel)
      }
      .setNegativeButton("取消", null)
      .show()
  }

  private fun showFileActionDialog(
    fileInfoModel: FileInfoModel,
    position: Int,
    filePageType: FilePageType
  ) {
    FileActionDialog(requireContext(), currentFocusedPosition, filePageType, targetFilePageType)
      .onItemClick = { itemPosition ->
      when (itemPosition) {
        "上传", "下载" -> showTransmissionDialog(fileInfoModel, position, filePageType)
        "分享" -> currentFileFragment.shareFile(position, fileInfoModel)
        "复制" -> copyFile(position, fileInfoModel)
        "移动" -> moveFile(position, fileInfoModel)
        "删除" -> currentFileFragment.deleteFile(position, fileInfoModel)
        "重命名" -> currentFileFragment.renameFile(position, fileInfoModel)
        "详情" -> currentFileFragment.showDetail(position, fileInfoModel)
      }
    }
  }

  override fun navigate(fileInfoModel: FileInfoModel, position: Int, filePageType: FilePageType) {
    if (fileInfoModel.isFile) {
      showFileActionDialog(fileInfoModel, position, filePageType)
      return
    }
    val path =
      if (filePageType == FilePageType.REMOTE) fileInfoModel.folderId else fileInfoModel.path
    addFilePage(fileInfoModel.name, filePageType, path)
  }

  private fun addFilePage(
    name: String,
    filePageType: FilePageType,
    path: String? = if (filePageType == FilePageType.REMOTE) "-1"
    else Environment.getExternalStorageDirectory().path,
    layoutPosition: LayoutPosition = currentFocusedPosition,
    fragment: FileFragment = UploadFileSelectorFragment.newInstance(path, layoutPosition)
  ) {
    val currentSelectedPosition = currentPaths.indexOfFirst { it.name == name }
    if (currentSelectedPosition != -1) {
      currentVp.setCurrentItem(currentSelectedPosition, true)
      toast("当前聚焦位置已存在相同选项卡，已跳转")
      return
    }
    val path = when (filePageType) {
      FilePageType.REMOTE -> RemotePathModel(
        path!!, name,
        LanzouFileFragment.newInstance(path, layoutPosition)
      )

      FilePageType.LOCAL -> LocalPathModel(path, name, fragment)
    }
    Log.i(TAG, "addFilePage: $name, $filePageType, $path $layoutPosition")
    val insertPosition = currentVp.currentItem + 1
    when (layoutPosition) {
      LayoutPosition.LEFT -> {
        leftPaths.add(insertPosition, path)
        currentLeftVpAdapter.notifyItemInserted(insertPosition)
        binding.vpLeft.setCurrentItem(insertPosition, true)
      }

      LayoutPosition.RIGHT -> {
        rightPaths.add(insertPosition, path)
        currentRightVpAdapter.notifyItemInserted(insertPosition)
        binding.vpRight.setCurrentItem(insertPosition, true)
      }

      else -> throw IllegalStateException()
    }
  }

  private fun removeFilePage(
    position: Int,
    layoutPosition: LayoutPosition = currentFocusedPosition
  ) {
    if (position < 0) {
      return
    }
    when (layoutPosition) {
      LayoutPosition.LEFT -> {
        if (leftPaths.size == 1) {
          toast("必须保留一个选项卡")
          return
        }
        leftPaths.removeAt(position)
        currentLeftVpAdapter.notifyItemRemoved(position)
      }

      LayoutPosition.RIGHT -> {
        if (rightPaths.size == 1) {
          toast("必须保留一个选项卡")
          return
        }
        rightPaths.removeAt(position)
        currentRightVpAdapter.notifyItemRemoved(position)
      }

      else -> throw IllegalStateException()
    }
  }

  /**
   * 上传文件
   *
   * @param path 文件绝对路径
   * @param folderId 要上传到的文件夹 id
   * @param folderName 要上传到的文件夹名称
   */
  private fun uploadFile(path: String, folderId: String, folderName: String) {
    val target = targetFileFragment
    uploadService.uploadFile(
      path,
      folderId.toLong(),
      folderName
    ) { upload ->
      if (view == null) {
        return@uploadFile
      }
      when (upload.status) {
        Upload.COMPLETE -> {
          target.getFile(upload.path)?.let {
            it.id = upload.fileId.toString()
            it.progress = 100
          }
        }

        Upload.PROGRESS -> {
          target.getFile(upload.path)?.let {
            it.progress = upload.progress
          }
        }

        Upload.ERROR -> {}
      }
    }
  }

  private fun downloadFile(directory: String, fileInfoModel: FileInfoModel) {
    val filePath = directory + File.separator + fileInfoModel.name
    val target = targetFileFragment
    fileInfoModel.path = filePath
    downloadService.addDownloadWithPath(
      fileInfoModel.id.toLong(),
      fileInfoModel.name,
      filePath
    ) { download ->
      if (view == null) {
        return@addDownloadWithPath
      }
      when (download.status) {
        Upload.PROGRESS, Upload.COMPLETE -> target.getFile(filePath)?.let {
          it.progress = download.progress
        }

        Upload.ERROR -> {}
      }
    }
  }

  private fun uploadFiles(path: String, checkedFiles: List<FileInfoModel>, folderName: String) {
    homeViewModel.toggleRight(false)
    val target = targetFileFragment
    val paths = checkedFiles.map { it.path }
    uploadService.uploadFiles(paths, path.toLong(), folderName) { upload ->
      if (view == null) {
        return@uploadFiles
      }
      when (upload.status) {
        Upload.COMPLETE -> {
          target.getFile(upload.path)?.let {
            it.id = upload.fileId.toString()
          }
        }

        Upload.PROGRESS -> {
          target.getFile(upload.path)?.let {
            it.progress = upload.progress
          }
        }

        Upload.ERROR -> {
          // FIXME: 不处理上传失败问题
        }
      }
    }
    targetFileFragment.addFiles(checkedFiles)
  }

  private fun downloadFiles(directory: String, checkedFiles: List<FileInfoModel>) {
    homeViewModel.toggleLeft(false)
    val target = targetFileFragment
    checkedFiles.forEach {
      it.path = directory + File.separator + it.name
    }
    downloadService.addDownload(checkedFiles) { download ->
      if (view == null) {
        return@addDownload
      }
      when (download.status) {
        Upload.PROGRESS, Upload.COMPLETE -> {
          target.getFile(download.path)?.let {
            it.progress = download.progress
          }
        }

        Upload.ERROR -> {
          // FIXME: 不处理下载失败问题
        }
      }
    }
    targetFileFragment.addFiles(checkedFiles)
  }

  private fun moveFile(position: Int, fileInfoModel: FileInfoModel) {
    scopeDialog {
      val currentPath = targetFileFragment.getCurrentPath()
      withIO {
        currentFileFragment.moveFile(position, fileInfoModel, currentPath)
      }?.let {
        currentFileFragment.removeFile(position, fileInfoModel)
        targetFileFragment.addFile(it)
      } ?: toast("发生错误或不能移动到这里")
    }
  }

  private fun moveFiles(checkedFiles: List<FileInfoModel>) {
    scopeDialog {
      val currentPath = targetFileFragment.getCurrentPath()
      checkedFiles.forEachIndexed { position, fileInfoModel ->
        withIO {
          currentFileFragment.moveFile(position, fileInfoModel, currentPath)
        }?.let {
          currentFileFragment.removeFile(-1, fileInfoModel)
          targetFileFragment.addFile(it)
        }
      }
    }.finally {
      homeViewModel.toggle()
    }
  }

  private fun copyFile(position: Int, fileInfoModel: FileInfoModel) {
    val targetPath = targetFileFragment.getCurrentPath()
    scopeDialog {
      // 暂时不显示复制的进度
      withIO {
        currentFileFragment.copyFile(position, fileInfoModel, targetPath)
      }?.let {
        targetFileFragment.addFile(it)
      } ?: toast("发生错误或不能复制到这里")
    }
  }

  private fun copyFiles(checkedFiles: List<FileInfoModel>) {
    // 只能本地对本地复制
    scopeDialog {
      val currentPath = targetFileFragment.getCurrentPath()
      checkedFiles.forEachIndexed { position, fileInfoModel ->
        withIO {
          currentFileFragment.copyFile(position, fileInfoModel, currentPath)
        }?.let {
          targetFileFragment.addFile(it)
        }
      }
    }.finally {
      homeViewModel.toggle()
    }
  }

  override fun onNavigateUp(): Boolean {
    if (currentPaths.size == 1) {
      return true
    }
    val position = currentVp.currentItem
    currentPaths.removeAt(position)
    currentVpAdapter.notifyItemRemoved(position)
    return false
  }

  override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
    if (p1 is DownloadService.DownloadBinder) {
      downloadService = p1.service
    } else if (p1 is UploadService.UploadBinder) {
      uploadService = p1.service
    }
  }

  override fun onServiceDisconnected(p0: ComponentName?) {

  }

  override fun onPrepareMenu(menu: Menu) {
    super.onPrepareMenu(menu)
    menu.findItem(R.id.show_system_app).isVisible = currentFileFragment is UploadAppSelectorFragment
    menu.findItem(R.id.sort).isVisible = currentFilePageType != FilePageType.REMOTE
    menu.findItem(R.id.copy).isVisible = currentFilePageType == FilePageType.LOCAL
        && currentFilePageType == targetFilePageType
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.menu_home, menu)
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    return when (menuItem.itemId) {
      R.id.scroll_top -> {
        currentFileFragment.scrollToPosition(0)
        true
      }

      R.id.copy -> {
        copyFiles(currentFileFragment.getCheckedFiles(false))
        true
      }

      R.id.show_system_app -> {
        menuItem.isChecked = !menuItem.isChecked
        homeViewModel.showSystemApp(menuItem.isChecked)
        currentRightFileFragment.refresh()
        true
      }

      R.id.add_tab -> {
        // TODO: 这里应该有一层映射关系，这里暂且写死
        val items = arrayOf("远程", "本地", "软件", "压缩包", "安装包", "视频")
        var currentPosition = 0
        MaterialAlertDialogBuilder(requireActivity())
          .setTitle("添加选项卡到当前聚焦位置")
          .setSingleChoiceItems(items, 0) { dialog, which ->
            currentPosition = which
          }
          .setPositiveButton("添加") { dialog, which ->
            val filePageType = if (currentPosition == 0) FilePageType.REMOTE else FilePageType.LOCAL
            val fragment = when (currentPosition) {
              0 -> LanzouFileFragment.newInstance(position = currentFocusedPosition)
              1 -> UploadFileSelectorFragment.newInstance(position = currentFocusedPosition)
              2 -> UploadAppSelectorFragment(position = currentFocusedPosition)
              3 -> UploadClassifySelectorFragment.newInstance(
                UploadClassifySelectorFragment.TYPE_ZIP,
                currentFocusedPosition
              )

              4 -> UploadClassifySelectorFragment.newInstance(
                UploadClassifySelectorFragment.TYPE_APK,
                currentFocusedPosition
              )

              5 -> UploadClassifySelectorFragment.newInstance(
                UploadClassifySelectorFragment.TYPE_VIDEO,
                currentFocusedPosition
              )

              else -> throw IllegalStateException()
            }
            val path = when (currentPosition) {
              0 -> "-1"
              1 -> Environment.getExternalStorageDirectory().path
              else -> null
            }
            addFilePage(items[currentPosition], filePageType, path = path, fragment = fragment)
          }
          .setNegativeButton("取消", null)
          .show()
        true
      }

      else -> currentFileFragment.onMenuItemSelected(menuItem)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    requireContext().unbindService(this)
    // uploadService.removeUploadListener(this)
  }

  private class FilePagerAdapter(
    private val paths: List<PathModel>,
    fm: FragmentManager,
    lifecycle: Lifecycle
  ) : FragmentStateAdapter(fm, lifecycle) {

    override fun createFragment(position: Int) = paths[position].fragment

    override fun getItemCount() = paths.size

    override fun getItemId(position: Int): Long {
      return paths[position].id
    }
  }

}