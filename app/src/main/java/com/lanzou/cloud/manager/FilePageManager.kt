package com.lanzou.cloud.manager

import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.model.LocalPathModel
import com.lanzou.cloud.model.PathModel
import com.lanzou.cloud.model.RemotePathModel
import com.lanzou.cloud.ui.fragment.AppFileFragment
import com.lanzou.cloud.ui.fragment.ClassifyFileFragment
import com.lanzou.cloud.ui.fragment.LanzouFileFragment
import com.lanzou.cloud.ui.fragment.PhoneFileFragment

class FilePageManager {

  /**
   * 路径列表
   */
  private val paths = mutableListOf(
    RemotePathModel(
      name = "远程",
      fragment = LanzouFileFragment.newInstance(name = "根目录")
    ),
    LocalPathModel(
      name = "本地",
      fragment = PhoneFileFragment.newInstance(position = LayoutPosition.LEFT)
    ),
    LocalPathModel(
      name = "本地",
      fragment = PhoneFileFragment.newInstance()
    ),
    RemotePathModel(
      name = "远程",
      fragment = LanzouFileFragment.newInstance(position = LayoutPosition.RIGHT, name = "根目录")
    ),
    LocalPathModel(name = "软件", fragment = AppFileFragment()),
    LocalPathModel(
      name = "压缩包",
      fragment = ClassifyFileFragment.newInstance(ClassifyFileFragment.TYPE_ZIP)
    ),
    LocalPathModel(
      name = "安装包",
      fragment = ClassifyFileFragment.newInstance(ClassifyFileFragment.TYPE_APK)
    ),
    LocalPathModel(
      name = "视频",
      fragment = ClassifyFileFragment.newInstance(ClassifyFileFragment.TYPE_VIDEO)
    ),
    LocalPathModel(
      name = "图片",
      fragment = ClassifyFileFragment.newInstance(ClassifyFileFragment.TYPE_IMAGE)
    ),
  )

  fun getPathsForPosition(position: LayoutPosition): List<PathModel> {
    return paths.filter { it.layoutPosition == position }
  }

  fun initPage(
    fragment: Fragment,
    layoutPosition: LayoutPosition,
    tab: TabLayout,
    vp: ViewPager2,
    onFilePageLayoutChangeListener: OnFilePageLayoutChangeListener
  ) {
    val currentPaths = getPathsForPosition(layoutPosition)
    vp.apply {
      offscreenPageLimit = 3
      isUserInputEnabled = false
      adapter = FilePagerAdapter(
        currentPaths,
        fragment.childFragmentManager,
        fragment.viewLifecycleOwner.lifecycle
      )
      registerOnPageChangeCallback(
        FilePageChangeCallback(
          currentPaths,
          onFilePageLayoutChangeListener
        )
      )
    }
    TabLayoutMediator(tab, vp) { tab, position ->
      tab.text = currentPaths[position].name
    }.attach()
    // tab.addOnTabSelectedListener(FileTabSelectedListener((layoutPosition)))
  }

  interface OnFilePageLayoutChangeListener {
    fun onFilePageSelected(position: Int, layoutPosition: LayoutPosition)
  }

  /**
   * 文件页面改变监听器
   */
  class FilePageChangeCallback(
    private val paths: List<PathModel>,
    private val onFilePageLayoutChangeListener: OnFilePageLayoutChangeListener
  ) :
    ViewPager2.OnPageChangeCallback() {

    override fun onPageSelected(position: Int) {
      onFilePageLayoutChangeListener.onFilePageSelected(position, paths[position].layoutPosition)
    }
  }

  /**
   * 文件标签选中监听器
   */
  private inner class FileTabSelectedListener(private val position: LayoutPosition) :
    TabLayout.OnTabSelectedListener {
    override fun onTabReselected(tab: TabLayout.Tab) {
      val popupMenu = PopupMenu(tab.view.context, tab.view)
      val menu = popupMenu.menu
      menu.add(0, 0, 0, "关闭")
      // menu.add(1, 1, 1, "复制")
      // menu.add(2, 2, 2,"关闭全部")
      popupMenu.show()
      popupMenu.setOnMenuItemClickListener {
        when (it.itemId) {
          /*0 -> removeFilePage(tab.position, position)
          1 -> {
            addFilePage(
              tab.text.toString(),
              targetFilePageType,
              currentPathModel.path!!,
              targetPosition,
            )
          }*/
        }
        true
      }
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
    }
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