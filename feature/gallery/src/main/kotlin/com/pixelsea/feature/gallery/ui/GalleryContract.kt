package com.pixelsea.feature.gallery.ui

import com.pixelsea.core.ui.architecture.ViewEffect
import com.pixelsea.core.ui.architecture.ViewEvent
import com.pixelsea.core.ui.architecture.ViewState

enum class GalleryPermissionState {
    Initial,
    Granted,
    Denied,
    PermanentlyDenied,
}

data class GalleryViewState(
    val permissionState: GalleryPermissionState = GalleryPermissionState.Initial,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEmpty: Boolean = false,
) : ViewState

sealed interface GalleryViewEvent : ViewEvent {
    data class PermissionResolved(
        val granted: Boolean,
        val shouldShowRationale: Boolean,
    ) : GalleryViewEvent

    data object PermissionRequestLaunched : GalleryViewEvent

    data object RetryLoadPhotos : GalleryViewEvent
}

sealed interface GalleryViewEffect : ViewEffect {
    data class ShowToast(val message: String) : GalleryViewEffect
}
