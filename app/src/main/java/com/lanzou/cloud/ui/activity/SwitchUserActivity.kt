package com.lanzou.cloud.ui.activity

import com.drake.brv.annotaion.AnimationType
import com.drake.brv.utils.dividerSpace
import com.drake.brv.utils.setup
import com.drake.net.utils.scope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanzou.cloud.R
import com.lanzou.cloud.base.BaseToolbarActivity
import com.lanzou.cloud.data.User
import com.lanzou.cloud.databinding.ActivitySwitchUserBinding
import com.lanzou.cloud.network.Repository
import kotlin.system.exitProcess

class SwitchUserActivity :
  BaseToolbarActivity<ActivitySwitchUserBinding>(R.layout.activity_switch_user) {
  override fun initData() {

  }

  override fun initView() {
    setTitle("账号管理")
    binding.refresh.onRefresh {
      scope {
        addData(Repository.getInstance().savedUserList)
      }
    }

    binding.userRv.dividerSpace(20).setup {
      setAnimation(AnimationType.SLIDE_RIGHT)
      addType<User>(R.layout.item_list_user)

      R.id.btn_select.onClick {
        Repository.getInstance().selectUser(getModel())
        binding.refresh.refresh()
        MaterialAlertDialogBuilder(this@SwitchUserActivity)
          .setTitle("操作成功")
          .setMessage("需要重启App生效")
          .setPositiveButton("退出App") { dialog, _ ->
            exitProcess(0)
          }
          .setNegativeButton("取消", null)
          .show()
      }
    }

    binding.btnAddUser.setOnClickListener {
      WebActivity.login()
    }
  }

  override fun onResume() {
    super.onResume()
    binding.refresh.refresh()
  }

}