package com.pixelsea.core.data.repository

import com.pixelsea.core.data.local.MediaStoreDataSource
import com.pixelsea.core.data.model.Photo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 照片数据仓库
 * 作为数据源的统一入口，向上层提供照片数据
 * @param mediaStoreDataSource 本地媒体存储数据源，负责从系统 MediaStore 获取照片
 */
class PhotoRepository @Inject constructor(
    private val mediaStoreDataSource: MediaStoreDataSource
) {
    // 暴露为 Flow 数据流，方便 UI 层响应式观察
    // 当底层数据变化时，Flow 会自动发送最新的数据给观察者
    fun getPhotosFlow(): Flow<List<Photo>> = flow {
        val photos = mediaStoreDataSource.getPhotos()
        emit(photos)
    }
}