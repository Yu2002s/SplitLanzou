package com.lanzou.cloud.model

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.drake.brv.annotaion.ItemOrientation
import com.drake.brv.item.ItemPosition
import com.drake.brv.item.ItemSwipe
import com.drake.engine.utils.PinyinUtils
import com.lanzou.cloud.BR
import com.lanzou.cloud.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class FileInfoModel(
  var id: String = "",
  @SerialName("fol_id")
  var folderId: String = "",
  @SerialName("name_all")
  val nameAll: String = "",
  var name: String = nameAll,
  @Transient
  var path: String = "",
  @Transient
  var length: Long = 0,
  var size: String = "",
  @SerialName("time")
  val updateTimeStr: String = "",
  @SerialName("icon")
  var extension: String? = null,
  @SerialName("is_lock")
  val lock: Int = 0,
  @SerialName("downs")
  val downloadCount: Int = 0,
  @Transient
  val pkgName: String? = null,
  @Transient
  val versionName: String = "",
  @Transient
  val updateTime: Long = 0,

  @Transient
  var highlight: Boolean = false,
  override var itemOrientationSwipe: Int = ItemOrientation.HORIZONTAL
) : Comparable<FileInfoModel>, BaseObservable(), ItemPosition, ItemSwipe {

  @Transient
  @get:Bindable
  var isChecked: Boolean = false
    set(value) {
      field = value
      notifyPropertyChanged(BR.checked)
    }

  @Transient
  @get:Bindable
  var isCheckable: Boolean = false
    set(value) {
      field = value
      notifyPropertyChanged(BR.checkable)
    }

  @get:Bindable("isChecked")
  val itemBg get() = if (isChecked) R.color.item_bg else android.R.color.transparent

  val nameTextColor get() = if (highlight) R.color.highlight else R.color.text_color

  val fileDesc: String
    get() {
      if (isApp) {
        return "$versionName - $size"
      }
      return if (downloadCount > 0) "$updateTimeStr $size ${downloadCount}下载" else "$updateTimeStr $size"
    }

  val isFile get() = extension != null

  val isDirectory get() = !isFile

  val isApp get() = pkgName != null

  val fileId get() = if (isFile) id else folderId

  override fun compareTo(other: FileInfoModel): Int {
    if (isApp && other.isApp) {
      return other.updateTime.compareTo(updateTime)
    }
    if (isDirectory && other.isFile) {
      return -1
    } else if (isFile && other.isDirectory) {
      return 1
    }

    return PinyinUtils.getPinyinFirstLetter(name)
      .compareTo(PinyinUtils.getPinyinFirstLetter(other.name))
  }

  override var itemPosition: Int = 0

}