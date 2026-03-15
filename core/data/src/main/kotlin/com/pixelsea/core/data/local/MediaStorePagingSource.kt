package com.pixelsea.core.data.local

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pixelsea.core.data.model.Photo
import com.pixelsea.core.data.util.MEDIASTORE_PHOTO_PROJECTION
import com.pixelsea.core.data.util.MEDIASTORE_PHOTO_SELECTION
import com.pixelsea.core.data.util.buildMediaStorePhotoQueryArgs
import com.pixelsea.core.data.util.buildMediaStorePhotoSortOrderWithPaging
import com.pixelsea.core.data.util.readMediaStorePhotoOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStorePagingSource(
    private val context: Context,
) : PagingSource<Int, Photo>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Photo> =
        withContext(Dispatchers.IO) {
            try {
                val offset = params.key ?: 0
                val loadSize = params.loadSize
                val photos = mutableListOf<Photo>()

                val cursor =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        context.contentResolver.query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            MEDIASTORE_PHOTO_PROJECTION,
                            buildMediaStorePhotoQueryArgs(limit = loadSize, offset = offset),
                            null,
                        )
                    } else {
                        context.contentResolver.query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            MEDIASTORE_PHOTO_PROJECTION,
                            MEDIASTORE_PHOTO_SELECTION,
                            null,
                            buildMediaStorePhotoSortOrderWithPaging(limit = loadSize, offset = offset),
                        )
                    }

                cursor?.use {
                    while (it.moveToNext()) {
                        it.readMediaStorePhotoOrNull()?.let(photos::add)
                    }
                }

                val nextKey = if (photos.isEmpty() || photos.size < loadSize) null else offset + photos.size
                val prevKey = if (offset == 0) null else maxOf(offset - loadSize, 0)

                LoadResult.Page(
                    data = photos,
                    prevKey = prevKey,
                    nextKey = nextKey,
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

    override fun getRefreshKey(state: PagingState<Int, Photo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(state.config.pageSize)
                ?: anchorPage?.nextKey?.minus(state.config.pageSize)?.coerceAtLeast(0)
        }
    }
}
