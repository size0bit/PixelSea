package com.pixelsea.core.data.repository

import com.pixelsea.core.data.local.MediaStoreDataSource
import com.pixelsea.core.data.model.Photo
import com.pixelsea.core.data.model.PhotoCollection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
class PhotoRepository
    @Inject
    constructor(
        private val mediaStoreDataSource: MediaStoreDataSource,
    ) {
        private val loadMutex = Mutex()

        @Volatile
        private var cachedCollection: PhotoCollection? = null

        suspend fun getAllPhotos(): List<Photo> {
            return getPhotoCollection().photos
        }

        suspend fun getPhotoCollection(forceRefresh: Boolean = false): PhotoCollection {
            val existing = cachedCollection
            if (!forceRefresh && existing != null) {
                return existing
            }

            return loadMutex.withLock {
                val lockedExisting = cachedCollection
                if (!forceRefresh && lockedExisting != null) {
                    return@withLock lockedExisting
                }

                val photos =
                    mediaStoreDataSource.getPhotos().sortedWith(
                        compareByDescending<Photo> { it.timestampMillis }
                            .thenByDescending { it.id },
                    )
                val collection =
                    PhotoCollection(
                        photos = photos,
                        indexById = photos.mapIndexed { index, photo -> photo.id to index }.toMap(),
                    )
                cachedCollection = collection
                collection
            }
        }
    }
