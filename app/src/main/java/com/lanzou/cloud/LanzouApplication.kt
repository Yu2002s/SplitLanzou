package com.lanzou.cloud;

import ando.file.core.FileOperator
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.view.View
import android.widget.TextView
import com.drake.brv.utils.BRV
import com.drake.engine.base.Engine
import com.drake.net.NetConfig
import com.drake.net.exception.HttpResponseException
import com.drake.net.exception.NetConnectException
import com.drake.net.exception.NetSocketTimeoutException
import com.drake.net.exception.NetworkingException
import com.drake.net.exception.RequestParamsException
import com.drake.net.interceptor.LogRecordInterceptor
import com.drake.net.okhttp.setConverter
import com.drake.net.okhttp.setDebug
import com.drake.net.okhttp.setDialogFactory
import com.drake.net.okhttp.setRequestInterceptor
import com.drake.statelayout.StateConfig
import com.drake.tooltip.dialog.BubbleDialog
import com.lanzou.cloud.config.Api
import com.lanzou.cloud.utils.converter.SerializationConverter
import com.lanzou.cloud.utils.interceptor.AppRequestInterceptor
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import org.litepal.LitePal
import java.util.concurrent.TimeUnit

/**
 * 第三方蓝奏云 (lanzou.com)
 * 支持上传 100M+ 文件
 */
class LanzouApplication : Application() {

  companion object {

    @SuppressLint("StaticFieldLeak")
    lateinit var context: Context

    const val HOST = "https://pc.woozooo.com/"

    const val HOST_FILE = HOST + "mydisk.php"

    const val HOST_LOGIN = HOST + "account.php?action=login"

    const val API_URL = "http://api.jdynb.xyz:6400"

    const val SHARE_URL = "http://lz.jdynb.xyz/index.html"

    const val SHARE_URL2 = "http://lzy.jdynb.xyz/share/file/"

    const val SHARE_FOLDER_URL = "http://lzy.jdynb.xyz/share/folder/"

    const val GITHUB_HOME = "https://github.com/Yu2002s/SplitLanzou"

    const val GITEE_HOME = "https://gitee.com/jdy2002/SplitLanzou"

    const val APP_SHARE_URL = "https://jdy2002.lanzoue.com/b041xpw2d"

    const val APP_SHARE_PWD = "2fgt"

    /**
     * 捐赠页，开发不易
     */
    const val APP_DONATE = "http://lzy.jdynb.xyz/donate"

  }

  override fun onCreate() {
    super.onCreate()
    context = this
    LitePal.initialize(this)
    FileOperator.init(this, false)

    BRV.modelId = BR.m // BRV 指定 databinding model name
    Engine.initialize(this) // 初始化引擎
    initNetConfig() // 初始化 Net 配置
    initStateConfig() // 初始化 StateLayout 配置
    initSmartRefreshLayout() // 初始化刷新组件
  }

  private fun initNetConfig() {
    // Net 初始化
    NetConfig.initialize(Api.BASE_URL, this) {
      // 连接超时时间
      connectTimeout(30, TimeUnit.SECONDS)
      // 读取超时时间
      readTimeout(30, TimeUnit.SECONDS)
      // 写入超时时间
      writeTimeout(30, TimeUnit.SECONDS)
      setDebug(BuildConfig.DEBUG)
      // 默认不开启缓存
      // cache(Cache(cacheDir, 1024 * 1024 * 128))
      // setConverter(GsonConverter())
      // 设置响应转换器，自动进行序列化和反序列化
      setConverter(SerializationConverter())
      // 添加日志拦截器
      addInterceptor(LogRecordInterceptor(BuildConfig.DEBUG))
      // addInterceptor(RefreshTokenInterceptor())
      // 设置请求拦截器
      setRequestInterceptor(AppRequestInterceptor())
      setDialogFactory {
        BubbleDialog(it).apply {
          setCancelable(false)
          setCanceledOnTouchOutside(false)
        }
      }
    }
  }

  private fun initStateConfig() {
    // StateLayout 初始化
    StateConfig.apply {
      loadingLayout = R.layout.layout_loading
      errorLayout = R.layout.layout_error
      emptyLayout = R.layout.layout_empty
      // 设置重试id
      setRetryIds(R.id.error_msg)
      onError { error ->
        startAnimation()
        findViewById<TextView>(R.id.error_msg).text = handleNetworkStatus(error)
      }
      onEmpty { tag ->
        if (tag is String) {
          findViewById<TextView>(R.id.empty_tips).text = tag
        }
        startAnimation()
      }
      onContent {
        // startAnimation()
      }
      onLoading {
        // startAnimation()
      }
    }
  }

  /**
   * 处理网络状态
   */
  private fun View.handleNetworkStatus(error: Any?): String {
    return when (error) {
      is NetworkingException -> getString(com.drake.net.R.string.net_error)
      is NetConnectException -> getString(com.drake.net.R.string.net_connect_error)
      is NetSocketTimeoutException -> getString(com.drake.net.R.string.net_connect_timeout_error)
      is RequestParamsException -> getString(com.drake.net.R.string.net_request_error)
      is HttpResponseException -> {
        when (error.response.code) {
          200 -> {
            getString(com.drake.net.R.string.net_other_error) + ", ${getString(R.string.refresh_tips)}"
          }

          500 -> getString(com.drake.net.R.string.net_server_error)
          else -> getString(R.string.error_tips)
        }
      }

      else -> getString(R.string.error_tips)
    }
  }

  private fun initSmartRefreshLayout() {
    SmartRefreshLayout.setDefaultRefreshHeaderCreator { _, _ -> MaterialHeader(this) }
    SmartRefreshLayout.setDefaultRefreshFooterCreator { _, _ -> ClassicsFooter(this) }
  }

  private fun View.startAnimation() {
    // 先将视图隐藏然后在800毫秒内渐变显示视图
    animate().setDuration(0).alpha(0F).withEndAction {
      animate().setDuration(800).alpha(1F)
    }
  }
}
