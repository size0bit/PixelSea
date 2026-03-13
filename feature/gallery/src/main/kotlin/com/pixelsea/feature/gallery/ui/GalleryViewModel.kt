package com.pixelsea.feature.gallery.ui

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import com.pixelsea.core.data.model.Photo
import com.pixelsea.core.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * 相册列表项的密封接口
 * 使用 sealed interface 确保类型安全，便于在 when 表达式中穷举所有情况
 */
sealed interface GalleryItem {
    /**
     * 照片项，包装单张照片数据
     * @param photo 照片实体对象
     */
    data class PhotoItem(val photo: Photo) : GalleryItem
    
    /**
     * 时间轴标题项，用于显示日期分组标题
     * @param date 格式化后的日期字符串，如 "2024年03月14日"
     */
    data class HeaderItem(val date: String) : GalleryItem
}

/**
 * 相册页面视图模型
 * 负责管理相册数据加载、时间轴分组和权限状态
 * 
 * @param photoRepository 照片数据仓库，用于获取分页照片数据
 */
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository
) : ViewModel() {

    /**
     * UI 状态的内部可变状态流
     * 使用 MutableStateFlow 实现响应式状态管理
     */
    private val _uiState = MutableStateFlow(GalleryViewState())
    
    /**
     * 对外暴露的只读状态流
     * UI 层通过此属性观察状态变化
     */
    val uiState: StateFlow<GalleryViewState> = _uiState.asStateFlow()

    /**
     * 分页照片数据流，包含时间轴标题
     * 
     * 数据处理流程：
     * 1. 从 Repository 获取原始 Photo 分页数据
     * 2. 将 Photo 包装成 PhotoItem
     * 3. 使用 insertSeparators 在不同日期的照片之间插入 HeaderItem
     * 
     * @return 包含时间轴标题的分页数据流
     */
    val pagedPhotos: Flow<PagingData<GalleryItem>> = photoRepository.getPagedPhotosFlow()
        .map { pagingData ->
            // 将原始 Photo 对象包装成 PhotoItem
            pagingData.map { GalleryItem.PhotoItem(it) }
        }
        .map { pagingData ->
            // 在相邻不同日期的照片之间插入日期标题
            pagingData.insertSeparators { before: GalleryItem.PhotoItem?, after: GalleryItem.PhotoItem? ->
                when {
                    // 列表末尾，不需要插入标题
                    after == null -> null
                    // 列表开头，插入第一个日期标题
                    before == null -> GalleryItem.HeaderItem(formatDate(after.photo.dateAdded))
                    // 检查前后照片日期是否相同
                    else -> {
                        val beforeDate = formatDate(before.photo.dateAdded)
                        val afterDate = formatDate(after.photo.dateAdded)
                        // 日期不同时插入新标题
                        if (beforeDate != afterDate) {
                            GalleryItem.HeaderItem(afterDate)
                        } else {
                            null
                        }
                    }
                }
            }
        }

    /**
     * 处理用户交互事件
     * 
     * @param event 用户事件，如权限申请结果
     */
    fun handleEvent(event: GalleryViewEvent) {
        when (event) {
            is GalleryViewEvent.PermissionResult -> {
                // 更新权限授予状态
                _uiState.update { it.copy(permissionGranted = event.granted) }
            }
        }
    }

    /**
     * 将时间戳格式化为可读日期字符串
     * 
     * @param seconds Unix 时间戳（秒）
     * @return 格式化后的日期字符串，如 "2024年03月14日"
     */
    private fun formatDate(seconds: Long): String {
        val formatter = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
        // MediaStore 的 dateAdded 单位是秒，需要转换为毫秒
        return formatter.format(Date(seconds * 1000))
    }
}