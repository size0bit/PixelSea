package com.pixelsea.core.data.model

data class PhotoCollection(
    val photos: List<Photo>,
    val indexById: Map<Long, Int>,
)
