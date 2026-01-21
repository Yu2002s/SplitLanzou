package com.lanzou.cloud.model

import androidx.databinding.PropertyChangeRegistry
import com.drake.brv.item.ItemExpand
import com.drake.engine.databinding.ObservableImpl
import com.lanzou.cloud.R
import org.litepal.annotation.Column
import org.litepal.crud.LitePalSupport

/**
 * 收藏夹
 */
data class FileFavoritesModel(
  /**
   * 收藏夹名称
   */
  @Column(nullable = false)
  val name: String = "",
  /**
   * 收藏项列表
   */
  val items: MutableList<FavoriteItem> = mutableListOf(),

  /**
   * 备注
   */
  @Column(nullable = true)
  val remark: String? = null,
  /**
   * 创建时间
   */
  val createAt: Long = System.currentTimeMillis(),
) : LitePalSupport(), ItemExpand, ObservableImpl {

  override val registry: PropertyChangeRegistry = PropertyChangeRegistry()

  val id: Long = 0L

  @Column(ignore = true)
  override var itemGroupPosition: Int = 0

  @Column(ignore = true)
  override var itemExpand: Boolean = false
    set(value) {
      field = value
      notifyChange()
    }

  override fun getItemSublist() = items

  val expandIcon get() = if (itemExpand) R.drawable.baseline_expand_more_24 else R.drawable.baseline_chevron_right_24

}

/**
 * 收藏项
 */
data class FavoriteItem(
  /**
   * 文件名
   */
  @Column(nullable = false)
  val name: String = "",
  /**
   * 文件 id
   */
  @Column(nullable = false, unique = true)
  val fileId: String = "",
  /**
   * 是否是文件
   */
  val isFile: Boolean = false,
  /**
   * 分享地址
   */
  @Column(nullable = false, unique = true)
  val url: String = "",
  /**
   * 文件密码
   */
  @Column(nullable = true)
  val pwd: String? = null,
  /**
   * 文件大小
   */
  @Column(nullable = true)
  val size: String? = null,
  /**
   * 文件扩展名
   */
  @Column(nullable = true)
  val extension: String? = null,
  /**
   * 分享时间
   */
  val time: String = "",
  /**
   * 备注
   */
  @Column(nullable = true)
  val remark: String? = null,
  val createAt: Long = System.currentTimeMillis(),
  val updateAt: Long = System.currentTimeMillis(),
  val favoritesModel: FileFavoritesModel = FileFavoritesModel(),
) : LitePalSupport(), ObservableImpl {

  override val registry: PropertyChangeRegistry = PropertyChangeRegistry()

  val id: Long = 0L

  val fileDesc get() = if (isFile) "$size $time" else "$pwd"
}