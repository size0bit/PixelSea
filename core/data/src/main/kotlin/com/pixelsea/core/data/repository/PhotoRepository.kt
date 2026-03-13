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

class PhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getPagedPhotosFlow(): Flow<PagingData<Photo>> {
        return Pager(
            config = PagingConfig(
                pageSize = 60,
                prefetchDistance = 20,
                enablePlaceholders = false,
                initialLoadSize = 120,
                maxSize = 100
            ),
            pagingSourceFactory = { MediaStorePagingSource(context) }
        ).flow
    }
}