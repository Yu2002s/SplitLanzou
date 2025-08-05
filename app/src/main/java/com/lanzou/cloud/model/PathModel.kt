package com.lanzou.cloud.model

import android.os.Environment
import com.lanzou.cloud.ui.fragment.FileFragment
import com.lanzou.cloud.ui.fragment.LanzouFileFragment
import com.lanzou.cloud.ui.fragment.UploadFileSelectorFragment
import kotlin.random.Random

sealed class PathModel(
  val name: String,
  val fragment: FileFragment,
  /**
   * 这个 path 可能为远程目录的 id，也可以是本地文件的路径
   */
  val path: String? = null,
) {

  var id: Long = System.currentTimeMillis() + Random.nextLong(999999)
}

/**
 * 远程路径
 */
class RemotePathModel(
  id: String = "-1",
  name: String,
  fragment: FileFragment = LanzouFileFragment.newInstance(id, name = "根目录")
) :
  PathModel(name, fragment, id)

/**
 * 本地路径
 */
class LocalPathModel(
  path: String? = Environment.getExternalStorageDirectory().path,
  name: String,
  fragment: FileFragment = UploadFileSelectorFragment.newInstance(path)
) :
  PathModel(name, fragment, path)