package com.pixelsea.core.data.util

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageValidator {
    fun isValidImageMimeType(mimeType: String?): Boolean {
        return !mimeType.isNullOrEmpty() && mimeType.startsWith("image/")
    }

    fun isCameraOrScreenshot(
        displayName: String?,
        bucketName: String?,
        relativePath: String?,
    ): Boolean {
        val normalizedName = displayName.orEmpty().lowercase()
        val normalizedBucket = bucketName.orEmpty().lowercase()
        val normalizedPath = relativePath.orEmpty().lowercase()

        val isScreenshot =
            normalizedBucket.contains("screenshot") ||
                normalizedBucket.contains("screen capture") ||
                normalizedPath.contains("/screenshots/") ||
                normalizedPath.contains("/screencaptures/") ||
                normalizedName.startsWith("screenshot") ||
                normalizedName.startsWith("screen_shot")

        val isCamera =
            normalizedBucket == "camera" ||
                normalizedPath.contains("/dcim/camera/") ||
                normalizedPath.endsWith("dcim/camera/")

        return isCamera || isScreenshot
    }

    suspend fun isValidImage(
        context: Context,
        uri: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val contentUri = Uri.parse(uri)
                val projection =
                    arrayOf(
                        MediaStore.Files.FileColumns.DISPLAY_NAME,
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                        MediaStore.Images.Media.RELATIVE_PATH,
                        MediaStore.Files.FileColumns.MIME_TYPE,
                    )

                context.contentResolver.query(contentUri, projection, null, null, null)?.use { cursor ->
                    if (!cursor.moveToFirst()) {
                        return@withContext false
                    }

                    val name =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME),
                        )
                    val bucketName =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME),
                        )
                    val relativePath =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH),
                        )
                    val mimeType =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE),
                        )

                    return@withContext isValidImageMimeType(mimeType) &&
                        isCameraOrScreenshot(name, bucketName, relativePath)
                }

                false
            } catch (_: Exception) {
                false
            }
        }
}
