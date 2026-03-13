package com.pixelsea.feature.gallery.ui

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import com.pixelsea.core.data.model.Photo
import com.pixelsea.core.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed interface GalleryItem {
    data class PhotoItem(val photo: Photo) : GalleryItem
    data class HeaderItem(val date: String) : GalleryItem
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryViewState())
    val uiState: StateFlow<GalleryViewState> = _uiState.asStateFlow()

    val pagedPhotos: Flow<PagingData<GalleryItem>> = photoRepository.getPagedPhotosFlow()
        .map { pagingData ->
            pagingData.map { GalleryItem.PhotoItem(it) }
        }
        .map { pagingData ->
            pagingData.insertSeparators { before: GalleryItem.PhotoItem?, after: GalleryItem.PhotoItem? ->
                if (after == null) {
                    null
                } else if (before == null) {
                    GalleryItem.HeaderItem(formatDate(after.photo.dateAdded))
                } else {
                    val beforeDate = formatDate(before.photo.dateAdded)
                    val afterDate = formatDate(after.photo.dateAdded)
                    if (beforeDate != afterDate) {
                        GalleryItem.HeaderItem(afterDate)
                    } else {
                        null
                    }
                }
            }
        }

    fun handleEvent(event: GalleryViewEvent) {
        when (event) {
            is GalleryViewEvent.PermissionResult -> {
                _uiState.update { it.copy(permissionGranted = event.granted) }
            }
        }
    }

    private fun formatDate(seconds: Long): String {
        val formatter = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
        return formatter.format(Date(seconds * 1000))
    }
}