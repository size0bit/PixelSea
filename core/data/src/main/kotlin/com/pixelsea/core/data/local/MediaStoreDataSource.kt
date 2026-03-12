package com.pixelsea.core.data.local

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.pixelsea.core.data.model.Photo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

// 告诉 Hilt 怎么构造这个类，并注入全局的 Context
/**
 * 媒体存储数据源
 * 负责从 Android 系统 MediaStore 查询和获取照片数据
 * 使用挂起函数确保查询操作在 IO 线程执行，避免阻塞主线程
 */
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 挂起函数：确保查询操作在 IO 线程执行，绝对不卡主线程
    // 使用 withContext(Dispatchers.IO) 将耗时操作移出主线程
    suspend fun getPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<Photo>()

        // 1. 定义我们要查哪些字段（投影）
        val projection = arrayOf(
            MediaStore.Images.Media._ID,       // 图片 ID
            MediaStore.Images.Media.DISPLAY_NAME, // 显示名称
            MediaStore.Images.Media.DATE_ADDED    // 添加日期
        )

        // 2. 按时间倒序排列（最新的照片在最前面）
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        // 3. 执行查询
        // 使用 ContentResolver 访问系统 MediaStore 数据库
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,   // 不设置筛选条件，获取所有图片
            null,   // 不设置筛选参数
            sortOrder // 按时间倒序
        )?.use { cursor ->
            // 提前获取列索引，提升遍历性能
            // 使用 getColumnIndexOrThrow 确保列存在，否则抛出异常
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            // 遍历游标，组装成我们的 Photo 对象
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)

                // 拼装出这张图片可访问的 Uri
                // 使用 ContentUris.withAppendedId 将 ID 附加到基础 URI 上
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                photos.add(
                    Photo(id = id, uri = contentUri.toString(), name = name, dateAdded = dateAdded)
                )
            }
        }
        return@withContext photos
    }
}