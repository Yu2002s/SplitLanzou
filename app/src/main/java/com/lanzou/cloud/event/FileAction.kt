package com.lanzou.cloud.event

import com.lanzou.cloud.model.FileInfoModel

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
}