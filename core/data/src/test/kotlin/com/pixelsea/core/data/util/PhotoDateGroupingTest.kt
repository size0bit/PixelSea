package com.pixelsea.core.data.util

import com.pixelsea.core.data.model.Photo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class PhotoDateGroupingTest {
    @Test
    fun `groups consecutive photos on the same day under one label`() {
        val photos =
            listOf(
                photo(id = 1L, timestampMillis = 1_710_086_400_000L),
                photo(id = 2L, timestampMillis = 1_710_082_800_000L),
                photo(id = 3L, timestampMillis = 1_710_000_000_000L),
            )

        val groups =
            groupPhotosByDate(
                photos = photos,
                locale = Locale.US,
                pattern = "yyyy-MM-dd",
                timeZone = TimeZone.getTimeZone("UTC"),
            )

        assertEquals(2, groups.size)
        assertEquals("2024-03-10", groups[0].label)
        assertEquals(listOf(1L, 2L), groups[0].photos.map { it.id })
        assertEquals("2024-03-09", groups[1].label)
        assertEquals(listOf(3L), groups[1].photos.map { it.id })
    }

    @Test
    fun `creates new group when day changes`() {
        val photos =
            listOf(
                photo(id = 10L, timestampMillis = 1_710_086_400_000L),
                photo(id = 11L, timestampMillis = 1_709_913_600_000L),
                photo(id = 12L, timestampMillis = 1_709_910_000_000L),
            )

        val groups =
            groupPhotosByDate(
                photos = photos,
                locale = Locale.US,
                pattern = "yyyy-MM-dd",
                timeZone = TimeZone.getTimeZone("UTC"),
            )

        assertEquals(2, groups.size)
        assertEquals("2024-03-10", groups[0].label)
        assertEquals("2024-03-08", groups[1].label)
        assertEquals(listOf(11L, 12L), groups[1].photos.map { it.id })
    }

    @Test
    fun `returns empty list for empty photos`() {
        assertTrue(
            groupPhotosByDate(
                emptyList(),
                locale = Locale.US,
                pattern = "yyyy-MM-dd",
                timeZone = TimeZone.getTimeZone("UTC"),
            ).isEmpty(),
        )
    }

    private fun photo(
        id: Long,
        timestampMillis: Long,
    ): Photo {
        return Photo(
            id = id,
            uri = "content://media/$id",
            name = "IMG_$id.jpg",
            timestampMillis = timestampMillis,
        )
    }
}
