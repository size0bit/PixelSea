package com.pixelsea.feature.viewer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelsea.core.data.model.Photo
import com.pixelsea.core.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViewerUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isEmpty: Boolean = false,
)

@HiltViewModel
class ViewerViewModel
    @Inject
    constructor(
        private val photoRepository: PhotoRepository,
    ) : ViewModel() {
        private val _photos = MutableStateFlow<List<Photo>>(emptyList())
        val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

        private val _uiState = MutableStateFlow(ViewerUiState())
        val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

        fun loadPhotos(forceRefresh: Boolean = false) {
            if (_uiState.value.isLoading) return

            viewModelScope.launch {
                _uiState.value = ViewerUiState(isLoading = true)
                runCatching {
                    photoRepository.getPhotoCollection(forceRefresh = forceRefresh).photos
                }.onSuccess { photos ->
                    _photos.value = photos
                    _uiState.value =
                        ViewerUiState(
                            isLoading = false,
                            isEmpty = photos.isEmpty(),
                        )
                }.onFailure {
                    _photos.value = emptyList()
                    _uiState.value =
                        ViewerUiState(
                            isLoading = false,
                            errorMessage = "读取图片失败，请返回后重试。",
                        )
                }
            }
        }

        init {
            _uiState.value = ViewerUiState(isLoading = false)
            loadPhotos()
        }
    }
