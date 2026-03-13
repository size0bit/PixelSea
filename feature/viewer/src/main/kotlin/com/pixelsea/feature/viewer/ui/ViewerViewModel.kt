package com.pixelsea.feature.viewer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pixelsea.core.data.model.Photo
import com.pixelsea.core.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ViewerViewModel @Inject constructor(
    photoRepository: PhotoRepository
) : ViewModel() {

    // 直接获取纯净的照片流，并缓存在 viewModelScope 中
    val pagedPhotos: Flow<PagingData<Photo>> =
        photoRepository.getPagedPhotosFlow().cachedIn(viewModelScope)
}