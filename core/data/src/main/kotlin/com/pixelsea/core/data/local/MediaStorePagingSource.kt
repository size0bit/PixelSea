package com.pixelsea.core.data.local

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pixelsea.core.data.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 基于 MediaStore 的分页数据源，用于从系统相册中分页加载图片
 *
 * @param context Android 上下文对象，用于访问 ContentResolver 查询媒体库
 * @return LoadResult 包含分页加载结果的密封类，成功时返回 Photo 列表和分页键，失败时返回异常
 */
class MediaStorePagingSource(
    private val context: Context
) : PagingSource<Int, Photo>() {

    /**
     * 加载单页数据的核心方法
     *
     * @param params 加载参数，包含页码（key）和每页大小（loadSize）
     * @return LoadResult 封装了三种可能的结果：Page（成功）、Error（失败）、Empty（空数据）
     */
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Photo> = withContext(Dispatchers.IO) {
        try {
            // 页码从 0 开始
            val pageNumber = params.key ?: 0
            val pageSize = params.loadSize
            val offset = pageNumber * pageSize

            val photos = mutableListOf<Photo>()
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )

            // 企业级适配：Android 11 (API 30) 及以上使用 Bundle 安全查询，以下使用 SQLite 语法糖
            val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val queryArgs = Bundle().apply {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, pageSize)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Images.Media.DATE_ADDED))
                    putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                }
                context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, queryArgs, null)
            } else {
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT $pageSize OFFSET $offset"
                context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder)
            }

            // 解析 Cursor 数据并转换为 Photo 实体列表
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn)
                    val dateAdded = it.getLong(dateAddedColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    photos.add(Photo(id, contentUri.toString(), name, dateAdded))
                }
            }

            // 判断是否还有下一页
            val nextKey = if (photos.isEmpty() || photos.size < pageSize) null else pageNumber + 1

            LoadResult.Page(
                data = photos,
                prevKey = if (pageNumber == 0) null else pageNumber - 1,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    /**
     * 获取刷新操作的起始页码
     *
     * @param state 当前分页状态，包含已加载的所有页面信息和锚点位置
     * @return Int? 刷新后应该定位到的页码，如果无法计算则返回 null
     */
    override fun getRefreshKey(state: PagingState<Int, Photo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}