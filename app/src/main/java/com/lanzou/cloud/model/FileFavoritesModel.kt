package com.lanzou.cloud.model

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
  val items: List<FavoriteItem> = emptyList(),

  /**
   * 备注
   */
  @Column(nullable = true)
  val remark: String? = null,
  /**
   * 创建时间
   */
  val createAt: Long = System.currentTimeMillis(),
) : LitePalSupport() {

  val id: Long = 0L
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
) : LitePalSupport() {

  val id: Long = 0L

}