package com.example.pixelsea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.pixelsea.ui.theme.PixelSeaTheme
import com.pixelsea.feature.gallery.ui.GalleryPhotoOpenRequest
import com.pixelsea.feature.gallery.ui.GalleryScreen
import com.pixelsea.feature.viewer.ui.ViewerScreen
import coil.imageLoader
import coil.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelSeaTheme {
                Surface(
                    modifier = Modifier,
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val context = LocalContext.current
                    var galleryFocusPhotoId by remember { mutableLongStateOf(-1L) }
                    var activeViewerRequest by remember { mutableStateOf<GalleryPhotoOpenRequest?>(null) }

                    SharedTransitionLayout {
                        Box(modifier = Modifier.fillMaxSize()) {
                            this@SharedTransitionLayout.run {
                                GalleryScreen(
                                    focusPhotoId = galleryFocusPhotoId.takeIf { it >= 0L },
                                    onPhotoClick = { request: GalleryPhotoOpenRequest ->
                                        context.imageLoader.enqueue(
                                            ImageRequest.Builder(context)
                                                .data(request.photoUri)
                                                .crossfade(false)
                                                .build(),
                                        )
                                        galleryFocusPhotoId = request.photoId
                                        activeViewerRequest = request
                                    },
                                    sharedTransitionScope = this,
                                    animatedVisibilityScope = null,
                                    sharedElementVisible = activeViewerRequest == null,
                                )
                            }

                            activeViewerRequest?.let { targetRequest ->
                                this@SharedTransitionLayout.run {
                                    ViewerScreen(
                                        initialPhotoId = targetRequest.photoId,
                                        initialPhotoUri = targetRequest.photoUri.toString(),
                                        onBackClick = { currentPhotoId ->
                                            galleryFocusPhotoId = currentPhotoId
                                            activeViewerRequest = null
                                        },
                                        sharedTransitionScope = this,
                                        animatedVisibilityScope = null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
