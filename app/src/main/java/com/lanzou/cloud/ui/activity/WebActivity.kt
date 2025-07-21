package com.lanzou.cloud.ui.activity

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import androidx.core.view.isInvisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanzou.cloud.LanzouApplication
import com.lanzou.cloud.R
import com.lanzou.cloud.base.BaseToolbarActivity
import com.lanzou.cloud.data.User
import com.lanzou.cloud.databinding.ActivityWebBinding
import com.lanzou.cloud.network.Repository
import com.lanzou.cloud.utils.formatBytes
import com.lanzou.cloud.utils.startActivity
import java.io.ByteArrayOutputStream
import java.io.IOException

class WebActivity : BaseToolbarActivity<ActivityWebBinding>(R.layout.activity_web) {

  companion object {

    private const val TAG = "WebActivity"

    private const val PARAM_URL = "url"

    private const val PARAM_LOGIN = "is_login"

    @JvmStatic
    fun actionStart(url: String) {
      startActivity<WebActivity>(PARAM_URL to url)
    }

    @JvmStatic
    fun login() {
      startActivity<WebActivity>(PARAM_URL to LanzouApplication.HOST_LOGIN, PARAM_LOGIN to true)
    }

  }

  private var isLogin = false

  override fun init() {
    super.init()
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if (binding.wv.canGoBack()) {
          binding.wv.goBack()
          return
        }
        onBack(binding.wv)
      }
    })
  }

  override fun initData() {
    isLogin = intent.getBooleanExtra(PARAM_LOGIN, false)
    if (isLogin) {
      binding.wv.addJavascriptInterface(HtmlSourceObj(), "local_obj")
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  override fun initView() {
    actionRight.text = "浏览器"
    actionRight.setOnClickListener {
      startActivity(Intent(Intent.ACTION_VIEW, binding.wv.url?.toUri()))
    }
    binding.wv.settings.apply {
      javaScriptEnabled = true
      javaScriptCanOpenWindowsAutomatically = true
      allowContentAccess = true
      allowFileAccess = true
      domStorageEnabled = true
      setSupportZoom(true)
      setSupportMultipleWindows(true)
      setSaveFormData(true)
      setUserAgentString("PC")
    }

    binding.wv.apply {
      loadUrl(intent.getStringExtra(PARAM_URL) ?: "")
      webViewClient = MyWebViewClient()
      webChromeClient = MyWebChromeClient()
      setDownloadListener { url, ua, contentDisposition, _, length ->
        MaterialAlertDialogBuilder(this@WebActivity)
          .setTitle("下载文件")
          .setMessage("是否下载该文件\n\n$contentDisposition\n\n${length.formatBytes()}")
          .setPositiveButton("下载") { _, _ ->
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
          }
          .setNegativeButton("取消", null)
          .show()
      }
    }
  }

  private inner class MyWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
      val url = view.url ?: return true
      if (url.startsWith("http")) {
        return false
      } else {
        try {
          startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: ActivityNotFoundException) {
          Log.e(TAG, "open external activity error: ${e.message}")
        }
      }
      return true
    }

    override fun onPageFinished(view: WebView, url: String) {
      if (isLogin && url == LanzouApplication.HOST_FILE) {
        val cookieManager = CookieManager.getInstance()
        val cookie = cookieManager.getCookie(url)
        if (cookie.contains("phpdisk_info=")) {
          val jsStr: String? = getJsStr()
          if (jsStr == null) {
            return
          }
          view.loadUrl("javascript:$jsStr")
        }
      }
    }
  }

  private fun getJsStr(): String? {
    try {
      val inputStream = assets.open("js/user.js")
      val bos = ByteArrayOutputStream()
      var len: Int
      val bytes = ByteArray(1024)
      while ((inputStream.read(bytes).also { len = it }) != -1) {
        bos.write(bytes, 0, len)
      }
      val str: String? = bos.toString()
      bos.close()
      inputStream.close()
      return str
    } catch (e: IOException) {
      e.printStackTrace()
    }
    return null
  }

  private inner class MyWebChromeClient : WebChromeClient() {
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
      binding.pg.setProgressCompat(newProgress, true)
      binding.pg.isInvisible = newProgress == 100
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
      setTitle(title)
    }

    override fun onJsAlert(
      view: WebView?,
      url: String?,
      message: String?,
      result: JsResult?
    ): Boolean {
      // 处理JS请求
      return super.onJsAlert(view, url, message, result)
    }
  }

  private inner class HtmlSourceObj {
    @JavascriptInterface
    fun saveUser(uid: Long, username: String?, cookie: String?) {
      runOnUiThread {
        MaterialAlertDialogBuilder(this@WebActivity)
          .setCancelable(false)
          .setTitle("保存用户信息")
          .setMessage("将对登录信息进行保存在本地，不会对个人信息进行上传云端，请放心使用")
          .setNeutralButton("关闭", null)
          .setPositiveButton(
            "确认保存"
          ) { dialog: DialogInterface?, which: Int ->
            val user = User()
            user.uid = uid
            user.cookie = cookie
            user.username = username
            user.isCurrent = true
            Repository.getInstance().saveOrUpdateUser(user)
            CookieManager.getInstance().removeAllCookies(null)
            setResult(RESULT_OK)
            finish()
          }.setNegativeButton(
            "退出"
          ) { dialog: DialogInterface?, which: Int -> finish() }
          .show()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    binding.wv.destroy()
  }
}