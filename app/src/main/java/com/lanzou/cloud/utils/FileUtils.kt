package com.lanzou.cloud.utils

import ando.file.core.FileUri
import android.content.Context
import android.webkit.MimeTypeMap
import androidx.core.app.ShareCompat
import com.drake.engine.utils.AppUtils
import com.drake.engine.utils.FileUtils
import com.drake.tooltip.toast
import com.lanzou.cloud.LanzouApplication
import com.lanzou.cloud.R
import java.io.File


object FileUtils {

  fun isMediaFile(ext: String?): Boolean {
    return "jpg" == ext || "webp" == ext || "png" == ext || "jpeg" == ext || "gif" == ext || "mp4" == ext
  }

  fun getIcon(ext: String?): Int {
    return when (ext) {
      null -> R.drawable.baseline_folder_24
      "zip", "jar" -> R.drawable.ic_archive
      "exe" -> R.drawable.ic_exe
      "apk" -> R.drawable.ic_apk
      "7z" -> R.drawable.ic_7z
      "rar" -> R.drawable.ic_rar
      "jpg" -> R.drawable.ic_jpg
      "png" -> R.drawable.ic_png
      "aac" -> R.drawable.ic_aac
      "mp3" -> R.drawable.ic_mp3
      "mp4" -> R.drawable.ic_mp4
      "sql" -> R.drawable.ic_db
      "docx" -> R.drawable.ic_doc
      "doc" -> R.drawable.ic_doc
      "pdf" -> R.drawable.ic_pdf
      "excel" -> R.drawable.ic_excel
      "gif" -> R.drawable.ic_gif
      "json" -> R.drawable.ic_json
      "ttf" -> R.drawable.ic_ttf
      "txt" -> R.drawable.ic_ttf
      "xml" -> R.drawable.ic_xml
      "kotlin" -> R.drawable.ic_kotlin
      else -> R.drawable.ic_file
    }
  }

  fun shareApp(context: Context, pkgName: String) {
    val appPath = AppUtils.getAppPath(pkgName)
    val appName = AppUtils.getAppName(pkgName)
    val targetPath = LanzouApplication.tempPath + "/$appName.apk"
    FileUtils.copyFile(appPath, targetPath) { true }
    shareFile(context, targetPath)
  }

  /**
   * 分享文件
   */
  fun shareFile(context: Context, path: String) {
    val file = File(path)
    if (file.exists()) {
      val shareUri = FileUri.getShareUri(path)
      ShareCompat.IntentBuilder(context)
        .setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension))
        .setStream(shareUri)
        .setChooserTitle("分享文件到")
        .startChooser()
    } else {
      toast("分享文件不存在")
    }
  }
}