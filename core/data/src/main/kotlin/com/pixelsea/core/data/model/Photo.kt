package com.pixelsea.core.data.model

/**
 * 核心领域模型：照片
 * 代表设备相册中的一张图片
 * @param id 照片在 MediaStore 中的唯一标识符
 * @param uri 照片的真实 Content Uri，后续传给 Coil 渲染
 * @param name 文件名
 * @param dateAdded 添加时间（用于时光轴排序）
 */
data class Photo(
    val id: Long,
    val uri: String,      // 照片的真实 Content Uri，后续传给 Coil 渲染
    val name: String,     // 文件名
    val dateAdded: Long   // 添加时间（用于时光轴排序）
)