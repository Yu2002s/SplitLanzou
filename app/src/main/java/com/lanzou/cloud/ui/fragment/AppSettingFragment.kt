package com.lanzou.cloud.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.lanzou.cloud.R
import com.lanzou.cloud.network.Repository
import com.lanzou.cloud.ui.activity.FolderSelectorActivity
import com.lanzou.cloud.utils.SpJavaUtils

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
          launcher.launch(Intent(requireContext(), FolderSelectorActivity::class.java))
          true
        }
    }
  }
}