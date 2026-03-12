package com.pixelsea.feature.gallery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelsea.core.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 相册页面的 ViewModel
 * 负责管理 UI 状态和处理用户交互
 * 使用 Hilt 进行依赖注入，确保生命周期感知
 */
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository
) : ViewModel() {

    // 内部状态存储，使用 MutableStateFlow 实现响应式更新
    private val _uiState = MutableStateFlow(GalleryViewState())
    // 对外暴露为只读的 StateFlow，确保状态只能由 ViewModel 修改
    val uiState: StateFlow<GalleryViewState> = _uiState.asStateFlow()

    /**
     * 处理用户意图（ViewEvent）
     * 根据用户的操作更新状态或触发业务逻辑
     */
    fun handleEvent(event: GalleryViewEvent) {
        when (event) {
            is GalleryViewEvent.PermissionResult -> {
                // 更新权限状态
                _uiState.update { it.copy(permissionGranted = event.granted) }
                if (event.granted) {
                    loadPhotos() // 权限通过，立刻加载本地照片！
                } else {
                    _uiState.update { it.copy(isLoading = false) } // 没给权限就结束 Loading
                }
            }
        }
    }

    /**
     * 加载照片数据
     * 在协程作用域内执行，确保不会阻塞主线程
     * 通过 Repository 获取照片数据流并更新 UI 状态
     */
    private fun loadPhotos() {
        viewModelScope.launch {
            // 设置加载中状态
            _uiState.update { it.copy(isLoading = true) }
            // 收集 Repository 发出的数据流
            photoRepository.getPhotosFlow().collect { photos ->
                _uiState.update {
                    it.copy(isLoading = false, photos = photos)
                }
            }
        }
    }
}