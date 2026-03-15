package com.pixelsea.core.data.model

/**
 * 核心领域模型：照片。
 *
 * @param id 照片在 MediaStore 中的唯一标识符
 * @param uri 照片的真实 Content Uri
 * @param name 文件名
 * @param timestampMillis 归一化后的展示时间（毫秒）
 */
data class Photo(
    val id: Long,
    val uri: String,
    val name: String,
    val timestampMillis: Long,
    val width: Int,
    val height: Int,
)
