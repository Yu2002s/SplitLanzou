package com.lanzou.cloud.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.drake.net.utils.scopeDialog
import com.drake.net.utils.withIO
import com.drake.tooltip.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanzou.cloud.LanzouApplication
import com.lanzou.cloud.R
import com.lanzou.cloud.config.SPConfig
import com.lanzou.cloud.model.ConfigModel
import com.lanzou.cloud.network.Repository
import com.lanzou.cloud.ui.activity.FolderSelectorActivity
import com.lanzou.cloud.utils.FileUtils
import com.lanzou.cloud.utils.SpJavaUtils
import com.lanzou.cloud.utils.SpUtils.getRequired
import com.lanzou.cloud.utils.SpUtils.put
import com.lanzou.cloud.utils.json
import okhttp3.OkHttpClient
import okhttp3.Request

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

    val uploadFileSizePreference = findPreference<Preference>(SPConfig.UPLOAD_FILE_SIZE)
    uploadFileSizePreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
      val sizeList = resources.getIntArray(R.array.file_size_values)
      val currentSize = SPConfig.UPLOAD_FILE_SIZE.getRequired<Int>(sizeList[1])
      val currentItem = sizeList.indexOf(currentSize)
      MaterialAlertDialogBuilder(requireContext())
        .setTitle("选择大小")
        .setSingleChoiceItems(R.array.file_size_entries, currentItem) { dialog, which ->
          SPConfig.UPLOAD_FILE_SIZE.put(sizeList[which])
          dialog.dismiss()
        }.setPositiveButton("关闭") { dialog, which ->
          dialog.dismiss()
        }
        .show()
      true
    }

    findPreference<Preference>("download_api_url")?.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        scopeDialog {
          val okHttpClient = OkHttpClient()
          val request = Request.Builder()
            .url(LanzouApplication.CONFIG_URL)
            .build()
          val response = withIO {
            okHttpClient.newCall(request).execute()
          }
          val responseBody = response.body?.string() ?: return@scopeDialog
          Log.i("AppSettingFragment", "responseBody: $responseBody")
          val configModel = json.decodeFromString<ConfigModel>(responseBody)
          val providers = configModel.download.provider
          val downloadApiUrl =
            SPConfig.DOWNLOAD_API_URL.getRequired<String>(LanzouApplication.DOWNLOAD_API_URL)
          val currentItem = providers.indexOfFirst { it.url == downloadApiUrl }
          MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择服务")
            .setSingleChoiceItems(
              providers.map { it.name }.toTypedArray(),
              currentItem
            ) { dialog, which ->
              SPConfig.DOWNLOAD_API_URL.put(providers[which].url)
              dialog.dismiss()
            }
            .setPositiveButton("关闭", null)
            .show()
        }
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