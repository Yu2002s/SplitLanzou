package com.lanzou.cloud.ui.fragment;

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.divider
import com.drake.brv.utils.setup
import com.drake.engine.base.EngineNavFragment
import com.drake.net.utils.scope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.king.camera.scan.CameraScan
import com.lanzou.cloud.LanzouApplication
import com.lanzou.cloud.R
import com.lanzou.cloud.data.User
import com.lanzou.cloud.databinding.FragmentMeBinding
import com.lanzou.cloud.databinding.ItemUserinfoBinding
import com.lanzou.cloud.model.OptionItem
import com.lanzou.cloud.model.UserInfoModel
import com.lanzou.cloud.network.LanzouRepository
import com.lanzou.cloud.network.Repository
import com.lanzou.cloud.ui.activity.AboutAppActivity
import com.lanzou.cloud.ui.activity.QRCodeScanActivity
import com.lanzou.cloud.ui.activity.QuestionActivity
import com.lanzou.cloud.ui.activity.RecycleFileActivity
import com.lanzou.cloud.ui.activity.ResolveFileActivity
import com.lanzou.cloud.ui.activity.ResolveFolderActivity
import com.lanzou.cloud.ui.activity.SettingActivity
import com.lanzou.cloud.ui.activity.UserProfileActivity
import com.lanzou.cloud.ui.activity.WebActivity
import com.lanzou.cloud.utils.startActivity
import kotlinx.coroutines.coroutineScope

class MeFragment : EngineNavFragment<FragmentMeBinding>(R.layout.fragment_me), MenuProvider {

  private var currentUser: User? = null

  private lateinit var launcher: ActivityResultLauncher<Intent>

  override fun initData() {
    launcher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
          val intent = result.data ?: return@registerForActivityResult
          val url =
            intent.getStringExtra(CameraScan.SCAN_RESULT) ?: return@registerForActivityResult
          // 扫码成功
          startActivity<ResolveFileActivity> {
            data = url.replace("https", "lanzou").toUri()
          }
        }
      }
  }

  override fun initView() {
    requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

    binding.meRv.divider {
      includeVisible = true
      orientation = DividerOrientation.VERTICAL
      setDivider(16, true)
    }.setup {
      addType<UserInfoModel>(R.layout.item_userinfo)
      addType<OptionItem>(R.layout.item_list_option)

      onCreate {
        getBindingOrNull<ItemUserinfoBinding>()?.let {
          it.tvProfile.setOnClickListener {
            startActivity<UserProfileActivity>()
          }
          it.btnLogin.setOnClickListener {
            WebActivity.login()
          }
        }
      }

      R.id.item_card.onClick {
        handleOptionItemClick(modelPosition)
      }
    }.models = getModels()

    binding.refreshLayout.onRefresh {
      // 加载用户的资料
      scope {
        getUser()
      }
    }.setEnableLoadMore(false)
  }

  private fun handleOptionItemClick(position: Int) {
    when (position) {
      0 -> startActivity<ResolveFileActivity>()
      1 -> startActivity<RecycleFileActivity>()
      2 -> WebActivity.actionStart(LanzouApplication.GITHUB_HOME)
      3 -> ResolveFolderActivity.resolve(
        LanzouApplication.APP_SHARE_URL,
        LanzouApplication.APP_SHARE_PWD
      )

      4 -> startActivity<QuestionActivity>()
      5 -> startActivity<AboutAppActivity>()
      6 -> startActivity<SettingActivity>()
    }
  }

  private fun getModels() = listOf(
    OptionItem(R.drawable.baseline_file_open_24, "解析文件"),
    OptionItem(R.drawable.baseline_recycling_24, "回收站"),
    OptionItem(R.drawable.baseline_download_for_offline_24, "获取更新"),
    OptionItem(R.drawable.baseline_history_24, "历史版本"),
    OptionItem(R.drawable.baseline_help_24, "一些问题"),
    OptionItem(R.drawable.baseline_logo_dev_24, "关于App"),
    // OptionItem(R.drawable.baseline_settings_24, "设置")
  )

  private fun showAboutDialog() {
    MaterialAlertDialogBuilder(requireContext())
      .setTitle("关于")
      .setMessage("软件仅供学习交流，请勿用于其他用途。不会自动更新，如需获取其他信息请访问github主页，如有问题请提issue\n\n作者:冬日暖雨")
      .setPositiveButton("关闭", null)
      .setNegativeButton(
        "github主页"
      ) { dialog: DialogInterface?, which: Int ->
        val intent = Intent(Intent.ACTION_VIEW, LanzouApplication.GITHUB_HOME.toUri())
        startActivity(intent)
      }
      .show()
  }

  private suspend fun getUser() = coroutineScope {
    val repository = Repository.getInstance()
    val user = repository.savedUser
    if (user != null && currentUser == user) {
      return@coroutineScope
    }

    val userInfoModel = UserInfoModel(user)
    if (user != null) {
      val profiles = LanzouRepository.getUserProfiles()
      userInfoModel.phone = profiles.find { it.name == "手机号" }?.value ?: ""
      userInfoModel.level = profiles.find { it.name == "我的会员" }?.value ?: ""
      userInfoModel.permission = profiles.find { it.name == "权限" }?.value ?: ""
    }
    binding.meRv.bindingAdapter.run {
      clearHeader()
      addHeader(userInfoModel)
    }
    currentUser = user
  }

  override fun onResume() {
    super.onResume()
    binding.refreshLayout.refreshing()
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.menu_me, menu)
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    if (menuItem.itemId == R.id.scan) {
      launcher.launch(Intent(requireContext(), QRCodeScanActivity::class.java))
      return true
    }
    return false
  }
}
