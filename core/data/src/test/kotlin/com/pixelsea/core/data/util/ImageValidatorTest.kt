package com.pixelsea.core.data.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageValidatorTest {
    @Test
    fun `accepts image mime types`() {
        assertTrue(ImageValidator.isValidImageMimeType("image/jpeg"))
        assertTrue(ImageValidator.isValidImageMimeType("image/png"))
    }

    @Test
    fun `rejects non image mime types`() {
        assertFalse(ImageValidator.isValidImageMimeType(null))
        assertFalse(ImageValidator.isValidImageMimeType(""))
        assertFalse(ImageValidator.isValidImageMimeType("video/mp4"))
    }

    @Test
    fun `recognizes camera photos`() {
        assertTrue(
            ImageValidator.isCameraOrScreenshot(
                displayName = "IMG_20260315_101010.jpg",
                bucketName = "Camera",
                relativePath = "DCIM/Camera/",
            ),
        )
    }

    @Test
    fun `recognizes screenshots by path and name`() {
        assertTrue(
            ImageValidator.isCameraOrScreenshot(
                displayName = "Screenshot_2026-03-15-10-10-10.png",
                bucketName = "Pictures",
                relativePath = "Pictures/Screenshots/",
            ),
        )
        assertTrue(
            ImageValidator.isCameraOrScreenshot(
                displayName = "screen_shot_001.png",
                bucketName = "Downloads",
                relativePath = "Downloads/",
            ),
        )
    }

    @Test
    fun `rejects unrelated images`() {
        assertFalse(
            ImageValidator.isCameraOrScreenshot(
                displayName = "wallpaper.png",
                bucketName = "Download",
                relativePath = "Download/",
            ),
        )
    }
}
