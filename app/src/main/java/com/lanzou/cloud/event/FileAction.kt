package com.lanzou.cloud.event

import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.model.FilterSortModel

/**
 * 一些文件操作的接口
 */
interface FileAction : Searchable {

  /**
   * 新建文件夹
   *
   * @param name 文件名称
   * @param path 文件路径
   */
  fun onMkdir(name: String, path: String)

  /**
   * 切换多选
   *
   * @param isMultiMode 是否多选
   */
  fun toggleMulti(isMultiMode: Boolean)

  /**
   * 获取选中的文件
   */
  fun getCheckedFiles(ignoreDirectory: Boolean = true): List<FileInfoModel>

  fun getCheckedPositions(): List<Int>

  /**
   * 添加文件到列表中
   */
  fun addFiles(files: List<FileInfoModel>)

  /**
   * 添加单个文件
   */
  fun addFile(fileInfoModel: FileInfoModel)

  /**
   * 添加文件
   */
  fun addFile(position: Int, fileInfoModel: FileInfoModel)

  /**
   * 刷新数据
   */
  fun refresh()

  /**
   * 删除指定文件
   *
   * @param positions 索引列表（注意这里的 position 是 layoutPosition，包含 header）
   * @param files 要删除的文件列表
   */
  fun deleteFiles(positions: List<Int>, files: List<FileInfoModel>)

  /**
   * 删除指定文件
   *
   * @param position 此 position 为 item 的 modelPosition，数据的实际位置
   * 不包含 header 区域，删除数据使用此 position，而实际更新 adapter 需要加上 headerCount
   * 可使用扩展方法 removeModel
   */
  fun deleteFile(position: Int, file: FileInfoModel)

  /**
   * 移除文件（实际不进行删除操作）
   */
  fun removeFile(position: Int, file: FileInfoModel)

  /**
   * 重命名文件
   *
   * @param position 索引（不包含 header 区域）
   * @param file 需要重命名的文件
   */
  fun renameFile(position: Int, file: FileInfoModel)

  /**
   * 滚动到指定位置
   *
   * @param position item 位置 (不包含 header)
   */
  fun scrollToPosition(position: Int)

  /**
   * 显示详情信息
   *
   * @param position item 位置 (不包含 header)
   * @param file 文件对象
   */
  fun showDetail(position: Int, file: FileInfoModel)

  /**
   * 排序文件
   *
   * @param filterSortModel 过滤规则
   */
  fun sort(filterSortModel: FilterSortModel)

  /**
   * 获取指定的文件
   *
   * @param path 文件路径
   * @return 文件对象
   */
  fun getFile(path: String): FileInfoModel?

  /**
   * 移动文件
   */
  suspend fun moveFile(position: Int, current: FileInfoModel, targetPath: String?): FileInfoModel?

  /**
   * 复制文件
   *
   * @return 新文件
   */
  fun copyFile(position: Int, current: FileInfoModel, targetPath: String?): FileInfoModel?

  /**
   * 分享文件
   */
  fun shareFile(position: Int, file: FileInfoModel)
}