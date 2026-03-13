package com.pixelsea.feature.gallery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.pixelsea.core.data.model.Photo
import com.pixelsea.core.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// 1. 定义混合的 UI 数据模型
sealed interface GalleryItem {
    data class PhotoItem(val photo: Photo) : GalleryItem
    data class HeaderItem(val date: String) : GalleryItem
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryViewState())
    val uiState: StateFlow<GalleryViewState> = _uiState.asStateFlow()

    // 2. 改造 Paging 数据流，动态插入日期标题
    val pagedPhotos: Flow<PagingData<GalleryItem>> = photoRepository.getPagedPhotosFlow()
        .map { pagingData ->
            // 先把所有的 Photo 包装成 PhotoItem
            pagingData.map { GalleryItem.PhotoItem(it) }
        }
        .map { pagingData ->
            // 在相邻的不同日期之间插入 HeaderItem
            pagingData.insertSeparators { before: GalleryItem.PhotoItem?, after: GalleryItem.PhotoItem? ->
                if (after == null) {
                    return@insertSeparators null // 列表最末尾不需要标题
                }
                if (before == null) {
                    // 列表最开头，无条件插入第一个标题
                    return@insertSeparators GalleryItem.HeaderItem(formatDate(after.photo.dateAdded))
                }

                // 对比前后两张照片的日期
                val beforeDate = formatDate(before.photo.dateAdded)
                val afterDate = formatDate(after.photo.dateAdded)

                if (beforeDate != afterDate) {
                    GalleryItem.HeaderItem(afterDate) // 日期发生变化，插入新标题
                } else {
                    null // 日期相同，不需要插标题
                }
            }
        }
        .cachedIn(viewModelScope)

    fun handleEvent(event: GalleryViewEvent) {
        when (event) {
            is GalleryViewEvent.PermissionResult -> {
                _uiState.update { it.copy(permissionGranted = event.granted) }
            }
        }
    }

    // 辅助方法：将时间戳转换成 "YYYY年MM月DD日" 格式
    private fun formatDate(seconds: Long): String {
        // MediaStore 的 dateAdded 单位是秒，需要乘以 1000 变成毫秒
        val formatter = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
        return formatter.format(Date(seconds * 1000))
    }
}