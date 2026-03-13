package com.pixelsea.core.data.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pixelsea.core.data.local.MediaStorePagingSource
import com.pixelsea.core.data.model.Photo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 照片数据仓库，负责提供分页加载照片数据的 Flow
 *
 * @param context Android 应用上下文，用于创建 MediaStorePagingSource 进行媒体库查询
 */
class PhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context // 直接注入 Context 来创建 PagingSource
) {
    /**
     * 获取分页照片数据流，用于在 UI 层观察和收集分页数据
     *
     * @return Flow<PagingData<Photo>> 冷流，每次收集时触发新的分页数据加载流程
     */
    // 之前那个可以删掉了，直接暴露 PagingData 的 Flow
    fun getPagedPhotosFlow(): Flow<PagingData<Photo>> {
        return Pager(
            config = PagingConfig(
                pageSize = 60, // 每次加载 60 张照片
                prefetchDistance = 20, // 距离底部还有 20 张时，自动触发预加载下一页
                enablePlaceholders = false,
                initialLoadSize = 120 // 第一次加载多一点，填满屏幕
            ),
            // 注意：每次 Pager 收集数据时，都必须返回一个新的 PagingSource 实例
            pagingSourceFactory = { MediaStorePagingSource(context) }
        ).flow
    }
}