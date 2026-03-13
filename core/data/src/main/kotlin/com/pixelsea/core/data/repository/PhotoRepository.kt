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
 * 照片数据仓库
 * 
 * 职责说明：
 * - 作为数据层的统一入口，为上层提供照片数据
 * - 封装 Paging 3 分页逻辑，实现高效的大数据集加载
 * - 通过 Hilt 注入，确保单例模式
 * 
 * @param context Android 应用上下文，用于访问 MediaStore
 */
class PhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 获取分页照片数据流
     * 
     * 返回一个冷流（Cold Flow），每次收集时触发新的数据加载。
     * 使用 Paging 3 库实现分页，支持：
     * - 自动加载更多数据
     * - 内存缓存优化
     * - 列表数据更新时自动刷新
     * 
     * @return Flow<PagingData<Photo>> 分页照片数据流
     */
    fun getPagedPhotosFlow(): Flow<PagingData<Photo>> {
        return Pager(
            config = PagingConfig(
                pageSize = 60,              // 每页加载 60 张照片
                prefetchDistance = 20,      // 距离底部 20 张时预加载下一页
                enablePlaceholders = false, // 禁用占位符，避免显示 null 项
                initialLoadSize = 120,      // 首次加载 120 张，填满屏幕
                maxSize = 100               // 最大缓存 100 张，控制内存占用
            ),
            // 每次创建新的 PagingSource，确保数据最新
            pagingSourceFactory = { MediaStorePagingSource(context) }
        ).flow
    }
}