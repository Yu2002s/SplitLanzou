package com.lanzou.cloud.ui.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lanzou.cloud.enums.LayoutPosition
import com.lanzou.cloud.model.HomeModel
import com.lanzou.cloud.network.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

  private val _homeModelFlow = MutableStateFlow(HomeModel())

  val homeModelFlow: StateFlow<HomeModel> = _homeModelFlow

  private val _focusedPositionFlow = MutableStateFlow(LayoutPosition.LEFT)

  val focusedPositionFlow: StateFlow<LayoutPosition> = _focusedPositionFlow

  private val _currentPositionFlow = MutableStateFlow(LayoutPosition.MIDDLE)

  val currentPositionFlow: StateFlow<LayoutPosition> = _currentPositionFlow

  var beforePosition = currentPositionFlow.value

  private val _leftMultiModelFlow = MutableStateFlow<Boolean?>(null)
  val leftMultiModeFlow: StateFlow<Boolean?> = _leftMultiModelFlow

  val leftMultiBtnText = MutableStateFlow("多选")

  private val _rightMultiModeFlow = MutableStateFlow<Boolean?>(null)
  val rightMultiModeFlow: StateFlow<Boolean?> = _rightMultiModeFlow

  val rightMultiBtnText = MutableStateFlow("多选")

  private val _loginStatusFlow = MutableStateFlow(true)
  val loginStateFlow: StateFlow<Boolean> = _loginStatusFlow

  init {

    // FIXME: 忘记快捷写法是咋写了 ...
    viewModelScope.launch {
      leftMultiModeFlow.collect {
        leftMultiBtnText.value = if (it == true) "取消" else "多选"
      }
    }

    viewModelScope.launch {
      rightMultiModeFlow.collect {
        rightMultiBtnText.value = if (it == true) "取消" else "多选"
      }
    }
  }

  fun movePosition(isLeft: Boolean = true) {
    val currentPosition = currentPositionFlow.value
    if (isLeft && currentPosition == LayoutPosition.LEFT) {
      return
    }
    if (!isLeft && currentPosition == LayoutPosition.RIGHT) {
      return
    }

    beforePosition = currentPosition

    _currentPositionFlow.value = when (currentPosition) {
      LayoutPosition.LEFT, LayoutPosition.RIGHT -> LayoutPosition.MIDDLE
      LayoutPosition.MIDDLE -> if (isLeft) LayoutPosition.LEFT else LayoutPosition.RIGHT
    }
  }

  fun changePosition(position: LayoutPosition) {
    _currentPositionFlow.value = position
  }

  fun focusPosition(positon: LayoutPosition) {
    _focusedPositionFlow.value = positon
  }

  fun toggleLeft() {
    _leftMultiModelFlow.value = !(_leftMultiModelFlow.value ?: false)
  }

  fun toggleLeft(toggleMode: Boolean) {
    _leftMultiModelFlow.value = toggleMode
  }

  fun toggleRight() {
    _rightMultiModeFlow.value = !(_rightMultiModeFlow.value ?: false)
  }

  fun toggleRight(toggleMode: Boolean) {
    _rightMultiModeFlow.value = toggleMode
  }

  fun refreshLogin() {
    _loginStatusFlow.value = Repository.getInstance().isLogin
  }
}