package com.lanzou.cloud

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.drake.tooltip.toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanzou.cloud.adapter.MainPageAdapter
import com.lanzou.cloud.config.SPConfig
import com.lanzou.cloud.data.LanzouPage
import com.lanzou.cloud.databinding.ActivityMainBinding
import com.lanzou.cloud.network.Repository
import com.lanzou.cloud.service.DownloadService
import com.lanzou.cloud.service.UploadService
import com.lanzou.cloud.ui.file.FileFragment
import com.lanzou.cloud.ui.selector.FileSelectorActivity
import com.lanzou.cloud.ui.selector.PhoneFileActivity
import com.lanzou.cloud.utils.SpJavaUtils
import com.lanzou.cloud.utils.UpdateUtils

/**
 * SplitLanzou
 *
 * @author 冬日暖雨
 * @mail jiangdongyu54@gmail.com
 * @since 2025/07/01
 */
class MainActivity : AppCompatActivity(), ServiceConnection {

  private lateinit var binding: ActivityMainBinding

  private var uploadService: UploadService? = null

  private var downloadService: DownloadService? = null

  @SuppressLint("NonConstantResourceId")
  protected override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false)
    bindService(Intent(this, UploadService::class.java), this, BIND_AUTO_CREATE)
    bindService(Intent(this, DownloadService::class.java), this, BIND_AUTO_CREATE)

    binding = ActivityMainBinding.inflate(getLayoutInflater())
    setContentView(binding.getRoot())
    setSupportActionBar(binding.header.toolBar)

    val viewPager2: ViewPager2 = binding.viewpager2
    viewPager2.setUserInputEnabled(false)
    viewPager2.setOffscreenPageLimit(5)
    viewPager2.setAdapter(MainPageAdapter(supportFragmentManager, lifecycle))

    viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
      override fun onPageSelected(position: Int) {
        binding.fab.visibility = if (position == 1) View.VISIBLE else View.INVISIBLE
      }
    })

    binding.bottomNav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED)
    binding.bottomNav.setOnItemSelectedListener({ item ->
      when (item.itemId) {
        R.id.nav_home -> viewPager2.setCurrentItem(0, false)
        R.id.nav_file -> viewPager2.setCurrentItem(1, false)
        R.id.nav_transmission -> viewPager2.setCurrentItem(2, false)
        R.id.nav_me -> viewPager2.setCurrentItem(3, false)
        R.id.nav_setting -> viewPager2.setCurrentItem(4, false)
      }
      invalidateMenu()
      true
    })

    val selectFileCallback: ActivityResultCallback<ActivityResult> =
      ActivityResultCallback { result ->
        val data: Intent? = result.data
        if (data != null && result.resultCode == RESULT_OK) {
          val files: ArrayList<CharSequence>? = data.getCharSequenceArrayListExtra("files")
          if (files == null || files.isEmpty()) {
            // 选择的文件为空时
            return@ActivityResultCallback
          }
          for (uri in files) {
            val currentPage: LanzouPage = this.fileFragment.currentPage
            uploadService?.uploadFile(
              uri.toString(),
              currentPage.folderId,
              currentPage.name
            )
          }
        }
      }

    val selectFileLauncher: ActivityResultLauncher<Intent> =
      registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        selectFileCallback
      )

    binding.fab.setOnClickListener({ view -> showUploadDialog(selectFileLauncher) })

    requestPermission()

    if (SpJavaUtils.getBoolean(SPConfig.CHECK_UPDATE, true)) {
      UpdateUtils.checkUpdate(this)
    }
  }

  private val fileFragment: FileFragment
    /**
     * 获取 FileFragment 实例
     *
     * @return FileFragment
     */
    get() {
      val fragments: MutableList<Fragment?> = supportFragmentManager.fragments
      val fragment: Fragment? = fragments[1]
      if (fragment !is FileFragment) {
        for (f in fragments) {
          if (f is FileFragment) {
            return f
          }
        }
        throw ClassCastException("获取 FileFragment 错误")
      }
      return fragment
    }

  private fun showUploadDialog(selectFileLauncher: ActivityResultLauncher<Intent>) {
    val repository = Repository.getInstance()

    val onClickListener: DialogInterface.OnClickListener =
      DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
        if (repository.uploadPath == null) {
          // 未选择
          toast(getString(R.string.tip_cache_path))
          return@OnClickListener
        }
        var clazz: Class<*>? = null
        when (which) {
          0 -> this.fileFragment.createFolder()
          1 -> clazz = FileSelectorActivity::class.java
          2 -> clazz = PhoneFileActivity::class.java
        }
        if (clazz != null) {
          selectFileLauncher.launch(Intent(this@MainActivity, clazz))
        }
        dialog.dismiss()
      }

    MaterialAlertDialogBuilder(this)
      .setTitle("选择操作")
      .setSingleChoiceItems(
        arrayOf<String>("新建文件夹", "分类选择上传", "文件选择上传"),
        -1,
        onClickListener
      )
      .show()
  }

  override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
    if (service is UploadService.UploadBinder) {
      uploadService = service.service
    } else if (service is DownloadService.DownloadBinder) {
      downloadService = service.service
    }
  }

  override fun onServiceDisconnected(name: ComponentName?) {
  }

  protected override fun onDestroy() {
    super.onDestroy()
    unbindService(this)
  }

  private fun requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      if (!Environment.isExternalStorageManager()) {
        try {
          toast("请授权此权限")
          val intent: Intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
          intent.setData(("package:$packageName").toUri())
          startActivity(intent)
        } catch (ignore: Exception) {
        }
      }
    } else {
      val granted: Int =
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
      if (granted != PackageManager.PERMISSION_GRANTED) {
        val permissions: Array<String> = arrayOf(
          Manifest.permission.WRITE_EXTERNAL_STORAGE,
          Manifest.permission.READ_EXTERNAL_STORAGE
        )
        ActivityCompat.requestPermissions(this, permissions, 1)
      }
    }
  }
}