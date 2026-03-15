package com.pixelsea.core.data.local

import android.content.Context
import android.provider.MediaStore
import com.pixelsea.core.data.model.Photo
import com.pixelsea.core.data.util.MEDIASTORE_PHOTO_PROJECTION
import com.pixelsea.core.data.util.MEDIASTORE_PHOTO_SELECTION
import com.pixelsea.core.data.util.MEDIASTORE_PHOTO_SORT_ORDER
import com.pixelsea.core.data.util.readMediaStorePhotoOrNull
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MediaStoreDataSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        suspend fun getPhotos(): List<Photo> =
            withContext(Dispatchers.IO) {
                val photos = mutableListOf<Photo>()

                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MEDIASTORE_PHOTO_PROJECTION,
                    MEDIASTORE_PHOTO_SELECTION,
                    null,
                    MEDIASTORE_PHOTO_SORT_ORDER,
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        cursor.readMediaStorePhotoOrNull()?.let(photos::add)
                    }
                }

                photos
            }
    }
