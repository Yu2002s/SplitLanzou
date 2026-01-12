package com.lanzou.cloud.ui.fragment

import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.os.bundleOf
import com.drake.tooltip.toast
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.utils.formatBytes
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 分类文件管理
 */
class ClassifyFileFragment(position: LayoutPosition = LayoutPosition.RIGHT) :
  LocalFileFragment(position) {

  companion object {

    private const val PARAM_TYPE = "type"

    const val TYPE_ZIP = 1

    const val TYPE_APK = 2

    const val TYPE_VIDEO = 3

    @JvmStatic
    fun newInstance(
      type: Int,
      position: LayoutPosition = LayoutPosition.RIGHT
    ): ClassifyFileFragment {
      val fragment = ClassifyFileFragment(position)
      fragment.arguments = bundleOf(PARAM_TYPE to type)
      return fragment
    }
  }

  override suspend fun getData(path: String?, page: Int): List<FileInfoModel>? {
    return getFiles()
  }

  override fun getFullPath(): String {
    return when (arguments?.getInt(PARAM_TYPE)) {
      TYPE_APK -> "安装包"
      TYPE_VIDEO -> "视频"
      TYPE_ZIP -> "压缩包"
      else -> super.getFullPath()
    }
  }

  override fun addFile(fileInfoModel: FileInfoModel) {
    toast("不能添加到这里")
  }

  override fun addFiles(files: List<FileInfoModel>) {
    toast("不能添加到这里")
  }

  private fun getMimeTypes(): Array<String> {
    return when (requireArguments().getInt(PARAM_TYPE, TYPE_ZIP)) {
      TYPE_ZIP -> arrayOf(
        getMimeTypeForExt("zip"),
        getMimeTypeForExt("7z"),
        getMimeTypeForExt("rar"),
        getMimeTypeForExt("tar"),
      )

      TYPE_APK -> arrayOf("application/vnd.android.package-archive")

      else -> emptyArray()
    }
  }

  private fun getMimeTypeForExt(ext: String): String {
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: ""
  }

  private fun getContentUri(): Uri {
    return when (requireArguments().getInt(PARAM_TYPE, TYPE_ZIP)) {
      TYPE_APK, TYPE_ZIP -> MediaStore.Files.getContentUri("external")
      TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
      else -> MediaStore.Files.getContentUri("external")
    }
  }

  private fun getFiles(): List<FileInfoModel>? {

    val contentResolver = requireContext().contentResolver
    val contentUri = getContentUri()
    val dateModified = MediaStore.Files.FileColumns.DATE_MODIFIED
    val mimeTypes = getMimeTypes()
    val sel = if (mimeTypes.isEmpty()) null else with(StringBuilder("mime_type = ?")) {
      repeat(mimeTypes.size - 1) {
        append(" or ")
        append("mime_type = ?")
      }
      toString()
    }
    val args = mutableListOf<String?>()
    mimeTypes.forEach { args.add(it) }
    val cursor =
      contentResolver.query(
        contentUri,
        null,
        sel,
        if (mimeTypes.isEmpty()) null else args.toTypedArray(),
        "$dateModified desc"
      )
    val nameIndex = cursor!!.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
    val dataIndex = cursor.getColumnIndexOrThrow("_data")
    val mimeTypeIndex = cursor.getColumnIndexOrThrow("mime_type")
    val titleIndex = cursor.getColumnIndexOrThrow("title")
    val dateModifiedIndex = cursor.getColumnIndexOrThrow(dateModified)
    val sizeIndex = cursor.getColumnIndexOrThrow("_size")

    val files = mutableListOf<FileInfoModel>()

    val simpleDataFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    cursor.use {
      while (it.moveToNext()) {
        val path = it.getString(dataIndex)
        // String uriString = ContentUris.withAppendedId(contentUri, id).toString();
        val name = it.getString(nameIndex)
        val mimeType = it.getString(mimeTypeIndex)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        val title = it.getString(titleIndex)
        val time = it.getLong(dateModifiedIndex)
        val timeStr = simpleDataFormat.format(time).substring(2)
        val size = it.getLong(sizeIndex)
        val fileName = name ?: "$title.$extension"
        files.add(
          FileInfoModel(
            name = fileName,
            size = size.formatBytes(),
            length = size,
            updateTimeStr = timeStr,
            updateTime = time,
            extension = extension,
            path = path,
          )
        )
      }
    }

    return files
  }
}