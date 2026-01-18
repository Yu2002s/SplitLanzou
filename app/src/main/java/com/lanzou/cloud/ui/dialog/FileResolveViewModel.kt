package com.lanzou.cloud.ui.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lanzou.cloud.LanzouApplication
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

  private val _clipData = MutableStateFlow<CharSequence>("")

  @OptIn(ExperimentalCoroutinesApi::class)
  val fileShareUrl = _clipData.map { text ->
    val fileRegex = "lanzou[a-z]*\\.com/(\\w+)".toRegex()
    fileRegex.find(text)
  }
    .filter { it != null }
    .flatMapLatest { matchResult ->
      val fileId = matchResult!!.destructured.component1()
      // val shareUrl = Repository.getInstance().getDownloadUrl()
      flowOf("${LanzouApplication.LANZOU_SHARE_BASE_URL}$fileId")
    }.catch {
      emit("")
    }
    .stateIn(viewModelScope, SharingStarted.Lazily, "")

  fun setClipData(data: CharSequence) {
    _clipData.value = data
  }
}