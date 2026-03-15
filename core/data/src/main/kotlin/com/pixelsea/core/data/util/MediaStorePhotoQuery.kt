package com.pixelsea.core.data.util

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import com.pixelsea.core.data.model.Photo

internal val MEDIASTORE_PHOTO_PROJECTION =
    arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.RELATIVE_PATH,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.MIME_TYPE,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
    )

internal val MEDIASTORE_PHOTO_SELECTION =
    buildString {
        append("${MediaStore.Images.Media.WIDTH} > 0")
        append(" AND ${MediaStore.Images.Media.HEIGHT} > 0")
        append(" AND (")
        append("LOWER(${MediaStore.Images.Media.BUCKET_DISPLAY_NAME}) = 'camera'")
        append(" OR LOWER(${MediaStore.Images.Media.BUCKET_DISPLAY_NAME}) LIKE '%screenshot%'")
        append(" OR LOWER(${MediaStore.Images.Media.BUCKET_DISPLAY_NAME}) LIKE '%screen capture%'")
        append(" OR LOWER(${MediaStore.Images.Media.RELATIVE_PATH}) LIKE '%/dcim/camera/%'")
        append(" OR LOWER(${MediaStore.Images.Media.RELATIVE_PATH}) LIKE '%/screenshots/%'")
        append(" OR LOWER(${MediaStore.Images.Media.RELATIVE_PATH}) LIKE '%/screencaptures/%'")
        append(" OR LOWER(${MediaStore.Images.Media.DISPLAY_NAME}) LIKE 'screenshot%'")
        append(" OR LOWER(${MediaStore.Images.Media.DISPLAY_NAME}) LIKE 'screen_shot%'")
        append(")")
    }

internal fun buildMediaStorePhotoQueryArgs(
    limit: Int,
    offset: Int,
): Bundle =
    Bundle().apply {
        putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
        putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, MEDIASTORE_PHOTO_SELECTION)
        putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, MEDIASTORE_PHOTO_SORT_ORDER)
    }

internal fun buildMediaStorePhotoSortOrderWithPaging(
    limit: Int,
    offset: Int,
): String {
    return "$MEDIASTORE_PHOTO_SORT_ORDER LIMIT $limit OFFSET $offset"
}

internal fun Cursor.readMediaStorePhotoOrNull(): Photo? {
    val columns = MediaStorePhotoColumns.from(this)
    val id = getLong(columns.id)
    val name = getString(columns.name)
    val bucketName = getString(columns.bucketName)
    val relativePath = getString(columns.relativePath)
    val dateAdded = getLong(columns.dateAdded)
    val dateTaken = getLong(columns.dateTaken)
    val mimeType = getString(columns.mimeType)
    val width = getInt(columns.width)
    val height = getInt(columns.height)

    if (!ImageValidator.isValidImageMimeType(mimeType)) {
        return null
    }
    if (!ImageValidator.isCameraOrScreenshot(name, bucketName, relativePath)) {
        return null
    }

    return Photo(
        id = id,
        uri =
            ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id,
            ).toString(),
        name = name,
        timestampMillis =
            resolvePhotoTimestampMillis(
                dateTaken = dateTaken,
                dateAdded = dateAdded,
                displayName = name,
            ),
        width = width,
        height = height,
    )
}

private data class MediaStorePhotoColumns(
    val id: Int,
    val name: Int,
    val bucketName: Int,
    val relativePath: Int,
    val dateAdded: Int,
    val dateTaken: Int,
    val mimeType: Int,
    val width: Int,
    val height: Int,
) {
    companion object {
        fun from(cursor: Cursor): MediaStorePhotoColumns {
            return MediaStorePhotoColumns(
                id = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID),
                name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME),
                bucketName = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME),
                relativePath = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH),
                dateAdded = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED),
                dateTaken = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN),
                mimeType = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE),
                width = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH),
                height = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT),
            )
        }
    }
}
