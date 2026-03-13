package com.pixelsea.feature.viewer.ui

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import com.pixelsea.core.data.model.Photo
import com.pixelsea.core.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ViewerViewModel @Inject constructor(
    photoRepository: PhotoRepository
) : ViewModel() {

    // 不使用缓存，确保每次进入都获取最新数据
    val pagedPhotos: Flow<PagingData<Photo>> =
        photoRepository.getPagedPhotosFlow()
}