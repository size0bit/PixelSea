package com.pixelsea.core.data.util

import android.provider.MediaStore
import java.time.LocalDateTime
import java.time.ZoneId

private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
private const val SECONDS_TO_MILLIS = 1000L
private const val SECONDS_THRESHOLD = 10_000_000_000L

private val SORT_DATE_TAKEN = MediaStore.Images.Media.DATE_TAKEN
private val SORT_DATE_ADDED = MediaStore.Images.Media.DATE_ADDED
private val SORT_ID = MediaStore.Images.Media._ID

private val CAMERA_NAME_PATTERN = Regex(""".*?(\d{4})(\d{2})(\d{2})[_-](\d{2})(\d{2})(\d{2}).*""")
private val SCREENSHOT_NAME_PATTERN =
    Regex(""".*?(\d{4})[-_]?(\d{2})[-_]?(\d{2})[-_](\d{2})[-_](\d{2})[-_](\d{2}).*""")

internal val MEDIASTORE_PHOTO_SORT_ORDER =
    buildString {
        append("CASE ")
        append("WHEN $SORT_DATE_TAKEN > 0 THEN ")
        append("CASE WHEN $SORT_DATE_TAKEN < $SECONDS_THRESHOLD THEN $SORT_DATE_TAKEN * $SECONDS_TO_MILLIS ELSE $SORT_DATE_TAKEN END ")
        append("WHEN $SORT_DATE_ADDED > 0 THEN ")
        append("CASE WHEN $SORT_DATE_ADDED < $SECONDS_THRESHOLD THEN $SORT_DATE_ADDED * $SECONDS_TO_MILLIS ELSE $SORT_DATE_ADDED END ")
        append("ELSE 0 END DESC, ")
        append("$SORT_ID DESC")
    }

internal fun resolvePhotoTimestampMillis(
    dateTaken: Long,
    dateAdded: Long,
    displayName: String? = null,
    nowMillis: Long = System.currentTimeMillis(),
): Long {
    return normalizeMediaStoreTimestamp(dateTaken, nowMillis)
        ?: resolveTimestampFromFileName(displayName, nowMillis)
        ?: normalizeMediaStoreTimestamp(dateAdded, nowMillis)
        ?: nowMillis
}

internal fun resolveTimestampFromFileName(
    displayName: String?,
    nowMillis: Long = System.currentTimeMillis(),
): Long? {
    val normalizedName = displayName.orEmpty()
    if (normalizedName.isBlank()) {
        return null
    }

    val match =
        SCREENSHOT_NAME_PATTERN.matchEntire(normalizedName)
            ?: CAMERA_NAME_PATTERN.matchEntire(normalizedName)
            ?: return null

    val timestamp =
        runCatching {
            LocalDateTime.of(
                match.groupValues[1].toInt(),
                match.groupValues[2].toInt(),
                match.groupValues[3].toInt(),
                match.groupValues[4].toInt(),
                match.groupValues[5].toInt(),
                match.groupValues[6].toInt(),
            )
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()

    return timestamp?.takeIf { it <= nowMillis + ONE_DAY_MILLIS }
}

private fun normalizeMediaStoreTimestamp(
    rawValue: Long,
    nowMillis: Long,
): Long? {
    if (rawValue <= 0L) {
        return null
    }

    val timestampMillis =
        if (rawValue < SECONDS_THRESHOLD) {
            rawValue * SECONDS_TO_MILLIS
        } else {
            rawValue
        }

    if (timestampMillis <= 0L) {
        return null
    }

    val maxReasonableTimestamp = nowMillis + ONE_DAY_MILLIS
    return timestampMillis.takeIf { it <= maxReasonableTimestamp }
}
