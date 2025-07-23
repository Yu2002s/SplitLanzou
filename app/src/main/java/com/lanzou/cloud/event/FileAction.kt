package com.lanzou.cloud.event

import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.model.FilterSortModel

interface FileAction : Searchable {

  /**
   * 新建文件夹
   */
  fun onMkdir(name: String, path: String)

  /**
   * 切换多选
   */
  fun toggleMulti(isMultiMode: Boolean)

  /**
   * 获取选中的文件
   */
  fun getCheckedFiles(): List<FileInfoModel>

  fun addFiles(files: List<FileInfoModel>)

  fun addFile(fileInfoModel: FileInfoModel)

  /**
   * 添加文件
   */
  fun addFile(position: Int, fileInfoModel: FileInfoModel)

  fun refresh()

  fun deleteFiles(positions: List<Int>, files: List<FileInfoModel>)

  /**
   * 删除指定文件
   *
   * @param position 此 position 为 item 的 modelPosition，数据的实际位置
   * 不包含 header 区域，删除数据使用此 position，而实际更新 adapter 需要加上 headerCount
   * 可使用扩展方法 removeModel
   */
  fun deleteFile(position: Int, file: FileInfoModel)

  fun renameFile(position: Int, file: FileInfoModel)

  fun scrollToPosition(position: Int)

  fun showDetail(position: Int, file: FileInfoModel)

  fun sort(filterSortModel: FilterSortModel)

  fun getFile(path: String): FileInfoModel?
}