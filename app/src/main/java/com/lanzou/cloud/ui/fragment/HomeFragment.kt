package com.lanzou.cloud.ui.fragment

import android.animation.ObjectAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.drake.engine.base.EngineNavFragment
import com.drake.engine.utils.dp
import com.drake.tooltip.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.lanzou.cloud.R
import com.lanzou.cloud.data.Upload
import com.lanzou.cloud.databinding.FragmentHomeBinding
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.event.OnFileNavigateListener
import com.lanzou.cloud.event.OnUploadListener
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.model.LanzouPathModel
import com.lanzou.cloud.service.DownloadService
import com.lanzou.cloud.service.UploadService
import com.lanzou.cloud.ui.dialog.FileMkdirDialog
import com.lanzou.cloud.ui.dialog.FileSearchDialog
import com.lanzou.cloud.utils.getWindowWidth
import kotlinx.coroutines.launch
import java.io.File

class HomeFragment : EngineNavFragment<FragmentHomeBinding>(R.layout.fragment_home),
  OnFileNavigateListener, ServiceConnection, OnUploadListener {

  private val leftFragments = mutableListOf<FileFragment>()

  private val rightFragments = mutableListOf<FileFragment>()

  private val lanzouFilePaths = mutableListOf(LanzouPathModel(name = "根目录"))
  private val fileTypes = arrayOf("根目录", "软件", "压缩包", "安装包", "视频")

  private lateinit var vpLeftAdapter: FilePagerAdapter
  private lateinit var vpRightAdapter: FilePagerAdapter

  private val windowWidth = getWindowWidth()

  private val leftLp by lazy {
    binding.leftContent.layoutParams as LinearLayout.LayoutParams
  }

  private val rightLp by lazy {
    binding.rightContent.layoutParams as LinearLayout.LayoutParams
  }

  private val homeViewModel by viewModels<HomeViewModel>()

  private val currentFileFragment
    get() = if (homeViewModel.focusedPositionFlow.value == LayoutPosition.LEFT) currentLeftFileFragment
    else currentRightFileFragment

  private val currentLeftFileFragment get() = leftFragments[binding.vpLeft.currentItem]

  private val currentRightFileFragment get() = rightFragments[binding.vpRight.currentItem]

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

    val contentWidth = (windowWidth - 1.dp) / 2
    leftLp.width = contentWidth
    rightLp.width = contentWidth

    vpRightAdapter = FilePagerAdapter(rightFragments, childFragmentManager, lifecycle)

    rightFragments.clear()
    rightFragments.add(UploadFileSelectorFragment())
    rightFragments.add(UploadAppSelectorFragment())
    rightFragments.add(UploadClassifySelectorFragment.newInstance(UploadClassifySelectorFragment.TYPE_ZIP))
    rightFragments.add(UploadClassifySelectorFragment.newInstance(UploadClassifySelectorFragment.TYPE_APK))
    rightFragments.add(UploadClassifySelectorFragment.newInstance(UploadClassifySelectorFragment.TYPE_VIDEO))

    vpLeftAdapter = FilePagerAdapter(leftFragments, childFragmentManager, lifecycle)
    leftFragments.clear()
    leftFragments.add(LanzouFileFragment.newInstance())

    val leftVp = binding.vpLeft
    leftVp.isUserInputEnabled = false
    leftVp.offscreenPageLimit = 2
    leftVp.adapter = vpLeftAdapter

    TabLayoutMediator(binding.tabLeft, leftVp) { tab, position ->
      tab.text = lanzouFilePaths[position].name
    }.attach()

    val rightVp = binding.vpRight
    rightVp.adapter = vpRightAdapter
    TabLayoutMediator(binding.tabRight, rightVp) { tab, position ->
      tab.text = fileTypes[position]
    }.attach()

    onBackPressedCallback =
      requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
        if (currentFileFragment.onBack()) {
          requireActivity().finish()
        } else {
          onBack()
        }
      }
  }

  private fun eachFragment(block: (FileFragment) -> Unit) {
    leftFragments.forEach(block)
    rightFragments.forEach(block)
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
            leftLp.width = currentLeftWidth - value
            rightLp.width = currentRightWidth + value
          }

          LayoutPosition.RIGHT -> {
            leftLp.width = currentLeftWidth + value
            rightLp.width = currentRightWidth - value
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
        val checkedFiles = currentRightFileFragment.getCheckedFiles()
        if (checkedFiles.isEmpty()) {
          toast("选中文件为空")
          return
        }
        val currentPath = currentLeftFileFragment.getCurrentPath() ?: return
        val currentFolderName = lanzouFilePaths[binding.vpLeft.currentItem].name

        MaterialAlertDialogBuilder(requireContext())
          .setTitle("上传")
          .setMessage("已选择${checkedFiles.size}个文件，将上传到: $currentFolderName\n\n【暂时不支持选择文件夹、不能显示进度】")
          .setPositiveButton("执行上传") { dialog, _ ->
            homeViewModel.toggleRight(false)
            uploadService.uploadFiles(
              checkedFiles
                .map {
                  it.highlight = true
                  it.path
                }, currentPath.toLong(), currentFolderName
            )
            currentLeftFileFragment.addFiles(ArrayList(checkedFiles))
          }
          .setNegativeButton("取消", null)
          .show()
      }

      R.id.fab_download -> {
        val checkedFiles = currentLeftFileFragment.getCheckedFiles()
        if (checkedFiles.isEmpty()) {
          toast("选中文件为空")
          return
        }
        val currentPath = currentRightFileFragment.getCurrentPath()
        if (currentPath == null) {
          toast("不能下载到当前位置")
          return
        }
        MaterialAlertDialogBuilder(requireContext())
          .setTitle("下载")
          .setMessage("已选择${checkedFiles.size}个文件，将下载到: $currentPath\n\n【暂时不支持选择文件夹，不能显示进度】")
          .setPositiveButton("执行下载") { dialog, _ ->
            homeViewModel.toggleLeft(false)
            checkedFiles.forEach {
              it.highlight = true
              it.path = currentPath + File.separator + it.name
            }
            currentRightFileFragment.addFiles(ArrayList(checkedFiles))
            downloadService.addDownload(checkedFiles)
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
        val path: String = if (currentPath.startsWith("/storage/emulated")) {
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

  private fun onBack() {

    if (currentFileFragment !is LanzouFileFragment) {
      // 过滤掉其他fragment执行返回
      return
    }

    if (lanzouFilePaths.size == 1) {
      requireActivity().finish()
      return
    }
    lanzouFilePaths.removeLastOrNull()
    leftFragments.removeLastOrNull()
    vpLeftAdapter.notifyItemRemoved(lanzouFilePaths.size)
  }

  override fun onResume() {
    super.onResume()
    onBackPressedCallback.isEnabled = true
  }

  override fun onPause() {
    super.onPause()
    onBackPressedCallback.isEnabled = false
  }

  private fun showTransmissionDialog(fileInfoModel: FileInfoModel, position: Int) {
    val isUpload = homeViewModel.focusedPositionFlow.value == LayoutPosition.RIGHT
    var message = "是否"
    message += if (isUpload) {
      "上传"
    } else {
      "下载"
    }
    message += fileInfoModel.name

    MaterialAlertDialogBuilder(requireContext())
      .setTitle("操作")
      .setMessage("$message\n\n注意：【暂时不能显示实时进度】")
      .setPositiveButton("执行") { dialog, _ ->
        if (isUpload) {
          val currentPath = currentLeftFileFragment.getCurrentPath() ?: return@setPositiveButton
          val currentFolderName = lanzouFilePaths[binding.vpLeft.currentItem].name
          currentLeftFileFragment.addFile(fileInfoModel.copy())
          uploadService.uploadFile(fileInfoModel.path, currentPath.toLong(), currentFolderName)
        } else {
          val currentPath = currentRightFileFragment.getCurrentPath() ?: return@setPositiveButton
          val filePath = currentPath + File.separator + fileInfoModel.name
          currentRightFileFragment.addFile(fileInfoModel)
          downloadService.addDownloadWithPath(
            fileInfoModel.id.toLong(),
            fileInfoModel.name,
            filePath
          )
        }
      }
      .setNegativeButton("取消", null)
      .show()
  }

  override fun navigate(fileInfoModel: FileInfoModel, position: Int) {
    if (fileInfoModel.isFile) {
      showTransmissionDialog(fileInfoModel, position)
      return
    }
    if (position == 0 && fileInfoModel.folderId.isEmpty()) {
      onBack()
      return
    }
    lanzouFilePaths.add(LanzouPathModel(fileInfoModel.folderId, fileInfoModel.name))
    leftFragments.add(LanzouFileFragment.newInstance(fileInfoModel.folderId))
    val insertPosition = lanzouFilePaths.size - 1
    vpLeftAdapter.notifyItemInserted(insertPosition)
    binding.vpLeft.post {
      binding.vpLeft.setCurrentItem(insertPosition, true)
    }
  }

  override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
    if (p1 is DownloadService.DownloadBinder) {
      downloadService = p1.service
    } else if (p1 is UploadService.UploadBinder) {
      uploadService = p1.service
      uploadService.addUploadListener(this)
    }
  }

  override fun onServiceDisconnected(p0: ComponentName?) {

  }

  override fun onUpload(upload: Upload?) {

    upload ?: return
    if (upload.status == Upload.ERROR) {
      currentLeftFileFragment.refresh()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    requireContext().unbindService(this)
    uploadService.removeUploadListener(this)
  }

  private class FilePagerAdapter(
    private val fragments: List<FileFragment>,
    fm: FragmentManager,
    lifecycle: Lifecycle
  ) : FragmentStateAdapter(fm, lifecycle) {

    override fun createFragment(position: Int) = fragments[position]

    override fun getItemCount() = fragments.size

  }

}