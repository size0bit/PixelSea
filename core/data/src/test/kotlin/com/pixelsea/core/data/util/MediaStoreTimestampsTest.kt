package com.pixelsea.core.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class MediaStoreTimestampsTest {
    @Test
    fun `prefers date taken when it is valid`() {
        val nowMillis = 1_710_000_000_000L
        val dateTakenMillis = nowMillis - 60_000L
        val dateAddedSeconds = (nowMillis / 1000L) - 120L

        val resolved =
            resolvePhotoTimestampMillis(
                dateTaken = dateTakenMillis,
                dateAdded = dateAddedSeconds,
                displayName = "Screenshot_2024-03-09-10-11-12.png",
                nowMillis = nowMillis,
            )

        assertEquals(dateTakenMillis, resolved)
    }

    @Test
    fun `falls back to file name timestamp before date added`() {
        val expected =
            LocalDateTime.of(2024, 3, 15, 10, 10, 10)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

        val resolved =
            resolvePhotoTimestampMillis(
                dateTaken = 0L,
                dateAdded = 1_800_000_000L,
                displayName = "Screenshot_2024-03-15-10-10-10.png",
                nowMillis = expected + 60_000L,
            )

        assertEquals(expected, resolved)
    }

    @Test
    fun `parses camera style file name timestamp`() {
        val resolved =
            resolveTimestampFromFileName(
                displayName = "IMG_20240315_101010.jpg",
                nowMillis = Long.MAX_VALUE / 2,
            )

        assertNotNull(resolved)
    }

    @Test
    fun `converts date added seconds into milliseconds`() {
        val nowMillis = 1_710_000_000_000L
        val dateAddedSeconds = 1_709_999_940L

        val resolved =
            resolvePhotoTimestampMillis(
                dateTaken = 0L,
                dateAdded = dateAddedSeconds,
                displayName = "plain_file_name.jpg",
                nowMillis = nowMillis,
            )

        assertEquals(dateAddedSeconds * 1000L, resolved)
    }

    @Test
    fun `falls back to now when all timestamp sources are invalid`() {
        val nowMillis = 1_710_000_000_000L

        val resolved =
            resolvePhotoTimestampMillis(
                dateTaken = 0L,
                dateAdded = nowMillis + (2 * 24 * 60 * 60 * 1000L),
                displayName = "screen_shot_invalid.png",
                nowMillis = nowMillis,
            )

        assertEquals(nowMillis, resolved)
    }

    @Test
    fun `sort order normalizes both date columns before ordering`() {
        assertTrue(MEDIASTORE_PHOTO_SORT_ORDER.contains("CASE"))
        assertTrue(MEDIASTORE_PHOTO_SORT_ORDER.contains("< 10000000000"))
        assertTrue(MEDIASTORE_PHOTO_SORT_ORDER.contains("* 1000"))
        assertTrue(MEDIASTORE_PHOTO_SORT_ORDER.contains("DESC"))
    }
}
