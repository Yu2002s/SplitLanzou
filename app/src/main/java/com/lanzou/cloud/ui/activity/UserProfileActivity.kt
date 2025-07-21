package com.lanzou.cloud.ui.activity

import com.drake.brv.annotaion.AnimationType
import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.divider
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.net.utils.scope
import com.drake.net.utils.scopeDialog
import com.drake.tooltip.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanzou.cloud.LanzouApplication
import com.lanzou.cloud.R
import com.lanzou.cloud.base.BaseEditDialog
import com.lanzou.cloud.base.BaseToolbarActivity
import com.lanzou.cloud.databinding.ActivityUserProfileBinding
import com.lanzou.cloud.model.ProfileModel
import com.lanzou.cloud.model.UserInfoModel
import com.lanzou.cloud.network.LanzouRepository
import com.lanzou.cloud.network.Repository
import com.lanzou.cloud.ui.dialog.EditPasswordDialog
import com.lanzou.cloud.utils.fitNavigationBar
import com.lanzou.cloud.utils.startActivity

class UserProfileActivity :
  BaseToolbarActivity<ActivityUserProfileBinding>(R.layout.activity_user_profile) {

  override fun initData() {
  }

  override fun initView() {
    setTitle(getString(R.string.title_user_center))

    binding.refreshLayout.onRefresh {
      scope {
        val userProfiles = LanzouRepository.getUserProfiles()
        binding.profileRv.models = userProfiles
        val userInfoModel = UserInfoModel(Repository.getInstance().savedUser, showProfile = false)
        userInfoModel.phone = userProfiles.find { it.name == "手机号" }?.value ?: ""
        userInfoModel.level = userProfiles.find { it.name == "我的会员" }?.value ?: ""
        userInfoModel.permission = userProfiles.find { it.name == "权限" }?.value ?: ""
        binding.profileRv.bindingAdapter.clearHeader()
        binding.profileRv.bindingAdapter.addHeader(userInfoModel)
      }
      setEnableLoadMore(false)
    }.showLoading()

    binding.profileRv.fitNavigationBar()

    binding.profileRv.divider {
      orientation = DividerOrientation.VERTICAL
      includeVisible = true
      setDivider(16, true)
    }.setup {
      setAnimation(AnimationType.SLIDE_RIGHT)
      addType<ProfileModel>(R.layout.item_list_profile)
      addType<Unit>(R.layout.item_logout)
      addType<UserInfoModel>(R.layout.item_userinfo)

      R.id.btn_switch.onClick {
        startActivity<SwitchUserActivity>()
      }

      R.id.btn_logout.onClick {
        MaterialAlertDialogBuilder(this@UserProfileActivity)
          .setTitle("退出登录")
          .setMessage("确认要退出登录嘛？")
          .setPositiveButton("确认") { dialog, _ ->
            Repository.getInstance().logout()
            finishTransition()
          }
          .setNegativeButton("取消", null)
          .show()
      }

      R.id.tv_value.onClick {
        val model = getModel<ProfileModel>()
        when (model.name) {
          "密码修改" -> EditPasswordDialog(this@UserProfileActivity)
          "个性域名" -> editDomain()
          "手机号" -> WebActivity.actionStart(LanzouApplication.HOST_FILE + "?item=profile&action=mypower")
          "注销账户" -> WebActivity.actionStart(LanzouApplication.HOST_FILE + "?item=profile&action=mypower")
        }

      }

      addFooter(Unit)
    }
  }

  private fun editDomain() {
    BaseEditDialog(this@UserProfileActivity).apply {
      setTitle("自定义域名")
      editValue = "自定义前缀"
      hint = "请修改域名前缀"
      setNegativeButton("取消", null)
      setPositiveButton("确认") { dialog, _ ->
        if (editValue.isBlank()) {
          toast("请输入前缀")
        } else {
          scopeDialog {
            LanzouRepository.editDomain(editValue)
            toast("修改成功")
          }.catch {
            toast(it.message)
          }
        }
      }
      show()
    }
  }


}