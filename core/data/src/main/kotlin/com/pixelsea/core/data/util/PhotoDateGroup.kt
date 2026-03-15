package com.pixelsea.core.data.util

import com.pixelsea.core.data.model.Photo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class PhotoDateGroup(
    val label: String,
    val photos: List<Photo>,
)

fun groupPhotosByDate(
    photos: List<Photo>,
    locale: Locale = Locale.getDefault(),
    pattern: String = "yyyy年MM月dd日",
    timeZone: TimeZone = TimeZone.getDefault(),
): List<PhotoDateGroup> {
    if (photos.isEmpty()) {
        return emptyList()
    }

    val formatter =
        SimpleDateFormat(pattern, locale).apply {
            this.timeZone = timeZone
        }
    val groups = ArrayList<PhotoDateGroup>()
    var currentLabel: String? = null
    var currentPhotos = mutableListOf<Photo>()

    photos.forEach { photo ->
        val label = formatter.format(Date(photo.timestampMillis))
        if (label != currentLabel) {
            if (currentLabel != null) {
                groups +=
                    PhotoDateGroup(
                        label = currentLabel.orEmpty(),
                        photos = currentPhotos.toList(),
                    )
            }
            currentLabel = label
            currentPhotos = mutableListOf(photo)
        } else {
            currentPhotos += photo
        }
    }

    if (currentLabel != null) {
        groups +=
            PhotoDateGroup(
                label = currentLabel.orEmpty(),
                photos = currentPhotos.toList(),
            )
    }

    return groups
}
