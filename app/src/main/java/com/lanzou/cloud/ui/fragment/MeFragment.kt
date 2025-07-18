package com.lanzou.cloud.ui.fragment;

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.drake.engine.base.EngineNavFragment
import com.lanzou.cloud.R
import com.lanzou.cloud.databinding.FragmentMeBinding
import com.lanzou.cloud.network.Repository
import com.lanzou.cloud.ui.dialog.UserDialog
import com.lanzou.cloud.ui.recycle.RecycleFileActivity
import com.lanzou.cloud.ui.resolve.ResolveFileActivity
import com.lanzou.cloud.utils.startActivity

class MeFragment : EngineNavFragment<FragmentMeBinding>(R.layout.fragment_me) {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_me, container, false)
  }

  override fun initData() {

  }

  override fun initView() {
    binding.btnManager.setOnClickListener {
      val userDialog = UserDialog(it.context);
      userDialog.setOnDismissListener {
        getUser()
      }
      userDialog.show()
    }

    binding.btnResolve.setOnClickListener {
      startActivity<ResolveFileActivity>()
    }

    binding.btnRecycle.setOnClickListener {
      startActivity<RecycleFileActivity>()
    }
  }

  private fun getUser() {
    val repository = Repository.getInstance();
    val user = repository.savedUser;
    binding.tvUsername.text = if (user == null) "未登录" else user.username;
  }

  override fun onResume() {
    super.onResume()
    getUser()
  }
}
