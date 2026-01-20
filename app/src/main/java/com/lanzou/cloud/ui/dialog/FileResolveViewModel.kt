package com.lanzou.cloud.ui.dialog

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drake.net.utils.withIO
import com.drake.tooltip.toast
import com.lanzou.cloud.LanzouApplication
import com.lanzou.cloud.model.LanzouResolveFileModel
import com.lanzou.cloud.network.LanzouRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FileResolveViewModel : ViewModel() {

  private val _clipData = MutableStateFlow<CharSequence?>(null)

  /**
   * 解析后的文件信息
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  val lanzouResolveFile = _clipData
    .filter { !it.isNullOrBlank() }
    .map { text ->
      val fileIdRegex = "lanzou[a-z]*\\.com/(\\w+)".toRegex()
      val pwdRegex = "密码[:：]*\\x20*(\\w+)".toRegex()
      arrayOf(fileIdRegex.find(text!!), pwdRegex.find(text))
    }
    .filter { it.first() != null }
    .flatMapLatest { matchResults ->
      val fileId = matchResults.first()!!.destructured.component1()
      val pwd = matchResults[1]?.destructured?.component1()
      Log.i("FileResolveViewModel", "fileId: $fileId, pwd: $pwd")
      val url = "${LanzouApplication.LANZOU_SHARE_BASE_URL}$fileId"
      // 获取文件的基本信息
      flowOf(getFileInfo(url, pwd))
    }.catch {
      Log.e("FileResolveViewModel", it.toString())
      toast("解析文件失败: ${it.message}")
    }
    .stateIn(viewModelScope, SharingStarted.Lazily, null)

  suspend fun getFileInfo(url: String, pwd: String? = null): LanzouResolveFileModel {
    return withIO {
      LanzouRepository.parseFile(url, pwd)
    }.getOrDefault(LanzouResolveFileModel(url = url, pwd = pwd))
  }

  /**
   * 设置剪切板数据
   */
  fun setClipData(data: CharSequence?) {
    _clipData.value = data
  }

  fun updatePwd() {
    _clipData.value ?: return
    val pwdRegex = "密码[:：]*\\x20*(\\w)*".toRegex()
    val pwd = lanzouResolveFile.value?.pwd ?: ""
    if (pwd.isEmpty()) {
      _clipData.value = _clipData.value.toString() + " "
      return
    }
    if (pwdRegex.containsMatchIn(_clipData.value!!)) {
      _clipData.value = _clipData.value!!.replace(pwdRegex, "密码:${pwd}")
    } else {
      _clipData.value = _clipData.value!!.toString() + " 密码:${pwd}"
    }
  }
}