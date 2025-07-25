package com.lanzou.cloud.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import com.drake.net.utils.scopeDialog
import com.drake.net.utils.withIO
import com.drake.tooltip.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanzou.cloud.R
import com.lanzou.cloud.config.SPConfig
import com.lanzou.cloud.network.Repository
import com.lanzou.cloud.ui.activity.FolderSelectorActivity
import com.lanzou.cloud.utils.FileUtils
import com.lanzou.cloud.utils.SpJavaUtils
import com.lanzou.cloud.utils.formatBytes

class AppSettingFragment : PreferenceFragmentCompat() {

  private var uploadCachePathPreference: Preference? = null

  override fun onCreatePreferences(
    savedInstanceState: Bundle?,
    rootKey: String?
  ) {
    setPreferencesFromResource(R.xml.preference_setting, rootKey)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val launcher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
          val data = result.data ?: return@registerForActivityResult
          val id = data.getLongExtra("id", -1)
          val folderName = data.getStringExtra("folderName")
          Repository.getInstance().updateUploadPath(id)
          uploadCachePathPreference?.summary = folderName
          SpJavaUtils.save("upload_cache_path_name", folderName)
        }
      }

    val uploadCachePathName = SpJavaUtils.get("upload_cache_path_name", "根目录")
    uploadCachePathPreference = findPreference<Preference>("upload_cache_path")?.apply {
      summary = uploadCachePathName
      onPreferenceClickListener =
        Preference.OnPreferenceClickListener {
          MaterialAlertDialogBuilder(requireContext())
            .setTitle("设置缓存目录")
            .setMessage("缓存用于分割文件时上传的目录，可以新建一个不使用的目录并设置它，将会上传大量文件到此处\n\n【不要设置到根目录】")
            .setPositiveButton("去设置") { dialog, _ ->
              launcher.launch(Intent(requireContext(), FolderSelectorActivity::class.java))
            }
            .setNegativeButton("取消", null)
            .show()
          true
        }
    }

    val uploadFileSizePreference = findPreference<SeekBarPreference>(SPConfig.UPLOAD_FILE_SIZE)
    uploadFileSizePreference?.onPreferenceChangeListener =
      Preference.OnPreferenceChangeListener { preference, newValue ->
        newValue as Int
        preference.summary = "当前设置: " + newValue.toLong().formatBytes()
        true
      }

    findPreference<Preference>("share_app")?.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        FileUtils.shareApp(requireContext(), requireContext().packageName)
        true
      }

    findPreference<Preference>("clear_cache")?.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        scopeDialog {
          withIO {
            requireContext().cacheDir?.deleteRecursively()
            requireContext().externalCacheDir?.deleteRecursively()
          }
          toast("清理完成")
        }
        true
      }
  }
}