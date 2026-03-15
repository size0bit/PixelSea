package com.pixelsea.feature.gallery.ui

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelsea.core.data.model.Photo
import com.pixelsea.core.data.model.PhotoCollection
import com.pixelsea.core.data.repository.PhotoRepository
import com.pixelsea.core.data.util.groupPhotosByDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GalleryItem {
    data class PhotoItem(val photo: Photo) : GalleryItem

    data class HeaderItem(val date: String) : GalleryItem
}

@HiltViewModel
class GalleryViewModel
    @Inject
    constructor(
        private val photoRepository: PhotoRepository,
    ) : ViewModel() {
        private var hasRequestedPermission = false

        private val _uiState = MutableStateFlow(GalleryViewState())
        val uiState: StateFlow<GalleryViewState> = _uiState.asStateFlow()

        private val _photoCollection = MutableStateFlow(PhotoCollection(emptyList(), emptyMap()))
        val photoCollection: StateFlow<PhotoCollection> = _photoCollection.asStateFlow()

        private val _galleryItems = MutableStateFlow<List<GalleryItem>>(emptyList())
        val galleryItems: StateFlow<List<GalleryItem>> = _galleryItems.asStateFlow()

        fun handleEvent(event: GalleryViewEvent) {
            when (event) {
                GalleryViewEvent.PermissionRequestLaunched -> {
                    hasRequestedPermission = true
                }

                GalleryViewEvent.RetryLoadPhotos -> {
                    loadPhotos(forceRefresh = true)
                }

                is GalleryViewEvent.PermissionResolved -> {
                    val permissionState =
                        when {
                            event.granted -> GalleryPermissionState.Granted
                            !hasRequestedPermission -> GalleryPermissionState.Initial
                            event.shouldShowRationale -> GalleryPermissionState.Denied
                            else -> GalleryPermissionState.PermanentlyDenied
                        }
                    _uiState.update { it.copy(permissionState = permissionState) }
                    if (permissionState != GalleryPermissionState.Granted) {
                        clearPhotos()
                    }
                }
            }
        }

        fun syncPermissionState(
            context: Context,
            activity: Activity?,
            permission: String,
        ) {
            val granted =
                ContextCompat.checkSelfPermission(
                    context,
                    permission,
                ) == PackageManager.PERMISSION_GRANTED
            val shouldShowRationale =
                activity?.shouldShowRequestPermissionRationale(permission) == true
            handleEvent(
                GalleryViewEvent.PermissionResolved(
                    granted = granted,
                    shouldShowRationale = shouldShowRationale,
                ),
            )
        }

        fun refreshIfGranted() {
            if (_uiState.value.permissionState == GalleryPermissionState.Granted) {
                loadPhotos(forceRefresh = true)
            }
        }

        private fun loadPhotos(forceRefresh: Boolean = false) {
            val currentState = _uiState.value
            if (currentState.permissionState != GalleryPermissionState.Granted || currentState.isLoading) {
                return
            }

            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        errorMessage = null,
                    )
                }

                runCatching {
                    photoRepository.getPhotoCollection(forceRefresh = forceRefresh)
                }.onSuccess { collection ->
                    _photoCollection.value = collection
                    _galleryItems.value = buildGalleryItems(collection.photos)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            isEmpty = collection.photos.isEmpty(),
                        )
                    }
                }.onFailure {
                    clearPhotos()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "读取相册失败，请重试。",
                            isEmpty = false,
                        )
                    }
                }
            }
        }

        private fun clearPhotos() {
            _photoCollection.value = PhotoCollection(emptyList(), emptyMap())
            _galleryItems.value = emptyList()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = null,
                    isEmpty = false,
                )
            }
        }

        private fun buildGalleryItems(photos: List<Photo>): List<GalleryItem> {
            if (photos.isEmpty()) {
                return emptyList()
            }

            return buildList {
                groupPhotosByDate(photos).forEach { group ->
                    add(GalleryItem.HeaderItem(group.label))
                    group.photos.forEach { photo ->
                        add(GalleryItem.PhotoItem(photo))
                    }
                }
            }
        }
    }
