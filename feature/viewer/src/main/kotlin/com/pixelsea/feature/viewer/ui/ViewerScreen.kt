package com.pixelsea.feature.viewer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale

@Suppress("FunctionName")
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ViewerScreen(
    initialPhotoId: Long,
    initialPhotoUri: String,
    onBackClick: (Long) -> Unit,
    viewModel: ViewerViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedElementVisible: Boolean = true,
) {
    val photos by viewModel.photos.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val density = LocalDensity.current
    val screenWidth = with(density) { 1.dp.toPx() * 360 }
    val swipeThreshold = screenWidth * 0.15f

    var currentIndex by remember { mutableStateOf(0) }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(initialPhotoId) {
        isInitialized = false
    }

    LaunchedEffect(photos, initialPhotoId, isInitialized) {
        if (photos.isNotEmpty() && !isInitialized) {
            currentIndex = photos.indexOfFirst { it.id == initialPhotoId }.takeIf { it >= 0 } ?: 0
            isInitialized = true
        }
    }

    BackHandler(enabled = photos.isNotEmpty() && isInitialized) {
        val safeIndex = currentIndex.coerceIn(0, photos.lastIndex)
        onBackClick(photos[safeIndex].id)
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading -> {
                ViewerOpeningPhoto(
                    photoId = initialPhotoId,
                    photoUri = initialPhotoUri,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedElementVisible = sharedElementVisible,
                )
            }

            uiState.errorMessage != null -> {
                ViewerStateContent(
                    title = "图片读取失败",
                    message = uiState.errorMessage ?: "读取图片失败，请返回后重试。",
                    actionLabel = "重新加载",
                    onActionClick = { viewModel.loadPhotos(forceRefresh = true) },
                )
            }

            uiState.isEmpty -> {
                ViewerStateContent(
                    title = "没有可查看的图片",
                    message = "当前没有找到可用的图片数据，请返回首页重新扫描。",
                    actionLabel = "返回首页",
                    onActionClick = { onBackClick(initialPhotoId) },
                )
            }

            photos.none { it.id == initialPhotoId } -> {
                ViewerOpeningPhoto(
                    photoId = initialPhotoId,
                    photoUri = initialPhotoUri,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedElementVisible = sharedElementVisible,
                )
            }

            photos.isNotEmpty() && isInitialized -> {
                var dragOffset by remember { mutableFloatStateOf(0f) }
                var isDragging by remember { mutableStateOf(false) }
                var isSwipingLeft by remember { mutableStateOf(false) }
                var scale by remember(currentIndex) { mutableFloatStateOf(1f) }
                var offset by remember(currentIndex) { mutableStateOf(Offset.Zero) }
                var viewportSize by remember(currentIndex) { mutableStateOf(Size.Zero) }

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { isDragging = true },
                                    onDrag = { change, dragAmount ->
                                        if (kotlin.math.abs(dragAmount.x) <= kotlin.math.abs(dragAmount.y)) {
                                            return@detectDragGestures
                                        }
                                        change.consume()
                                        dragOffset += dragAmount.x
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        if (dragOffset > swipeThreshold && currentIndex > 0) {
                                            isSwipingLeft = true
                                            currentIndex--
                                        } else if (dragOffset < -swipeThreshold && currentIndex < photos.lastIndex) {
                                            isSwipingLeft = false
                                            currentIndex++
                                        }
                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        dragOffset = 0f
                                    },
                                )
                            },
                ) {
                    AnimatedContent(
                        targetState = currentIndex,
                        transitionSpec = {
                            if (isSwipingLeft) {
                                slideInHorizontally(tween(300)) { -it } togetherWith
                                    slideOutHorizontally(tween(300)) { it }
                            } else {
                                slideInHorizontally(tween(300)) { it } togetherWith
                                    slideOutHorizontally(tween(300)) { -it }
                            }
                        },
                        label = "viewerPager",
                    ) { index ->
                        val safeIndex = index.coerceIn(0, photos.lastIndex)
                        val photo = photos[safeIndex]
                        var imageLoadFailed by remember(photo.id) { mutableStateOf(false) }

                        if (imageLoadFailed) {
                            ViewerStateContent(
                                title = "图片加载失败",
                                message = "这张图片暂时无法显示。你可以继续滑动查看下一张，或返回首页重试。",
                                actionLabel = "返回首页",
                                onActionClick = { onBackClick(photo.id) },
                            )
                        } else {
                            val transformableState =
                                rememberTransformableState { zoomChange, panChange, _ ->
                                    val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
                                    scale = nextScale

                                    if (scale <= 1f) {
                                        offset = Offset.Zero
                                    } else {
                                        val maxOffsetX = (viewportSize.width * (scale - 1f)) / 2f
                                        val maxOffsetY = (viewportSize.height * (scale - 1f)) / 2f
                                        val nextOffset = offset + panChange
                                        offset =
                                            Offset(
                                                x = nextOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                                                y = nextOffset.y.coerceIn(-maxOffsetY, maxOffsetY),
                                            )
                                    }
                                }

                            val imageModifier =
                                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                    with(sharedTransitionScope) {
                                        Modifier
                                            .fillMaxSize()
                                            .onSizeChanged { size ->
                                                viewportSize = Size(size.width.toFloat(), size.height.toFloat())
                                            }
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onDoubleTap = { tapOffset ->
                                                        if (scale > 1f) {
                                                            scale = 1f
                                                            offset = Offset.Zero
                                                        } else {
                                                            scale = 2.5f
                                                            val maxOffsetX = (viewportSize.width * (scale - 1f)) / 2f
                                                            val maxOffsetY = (viewportSize.height * (scale - 1f)) / 2f
                                                            offset =
                                                                Offset(
                                                                    x = ((viewportSize.width / 2f) - tapOffset.x).coerceIn(-maxOffsetX, maxOffsetX),
                                                                    y = ((viewportSize.height / 2f) - tapOffset.y).coerceIn(-maxOffsetY, maxOffsetY),
                                                                )
                                                        }
                                                    },
                                                )
                                            }
                                            .transformable(
                                                state = transformableState,
                                                canPan = { scale > 1f },
                                            )
                                            .graphicsLayer(
                                                scaleX = scale,
                                                scaleY = scale,
                                                translationX = offset.x,
                                                translationY = offset.y,
                                            )
                                            .let { modifier ->
                                                if (animatedVisibilityScope != null) {
                                                    modifier.sharedElement(
                                                        state = rememberSharedContentState(key = "photo-${photo.id}"),
                                                        animatedVisibilityScope = animatedVisibilityScope,
                                                    )
                                                } else {
                                                    modifier.sharedElementWithCallerManagedVisibility(
                                                        sharedContentState = rememberSharedContentState(key = "photo-${photo.id}"),
                                                        visible = sharedElementVisible,
                                                    )
                                                }
                                            }
                                    }
                                } else {
                                    Modifier
                                        .fillMaxSize()
                                        .onSizeChanged { size ->
                                            viewportSize = Size(size.width.toFloat(), size.height.toFloat())
                                        }
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onDoubleTap = { tapOffset ->
                                                    if (scale > 1f) {
                                                        scale = 1f
                                                        offset = Offset.Zero
                                                    } else {
                                                        scale = 2.5f
                                                        val maxOffsetX = (viewportSize.width * (scale - 1f)) / 2f
                                                        val maxOffsetY = (viewportSize.height * (scale - 1f)) / 2f
                                                        offset =
                                                            Offset(
                                                                x = ((viewportSize.width / 2f) - tapOffset.x).coerceIn(-maxOffsetX, maxOffsetX),
                                                                y = ((viewportSize.height / 2f) - tapOffset.y).coerceIn(-maxOffsetY, maxOffsetY),
                                                            )
                                                    }
                                                },
                                            )
                                        }
                                        .transformable(
                                            state = transformableState,
                                            canPan = { scale > 1f },
                                        )
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y,
                                        )
                                }

                            AsyncImage(
                                model =
                                    ImageRequest.Builder(LocalContext.current)
                                        .data(photo.uri)
                                        .placeholderMemoryCacheKey("thumb-${photo.id}")
                                        .scale(Scale.FIT)
                                        .crossfade(false)
                                        .build(),
                                contentDescription = photo.name,
                                contentScale = ContentScale.Fit,
                                onError = {
                                    imageLoadFailed = true
                                },
                                modifier = imageModifier,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ViewerOpeningPhoto(
    photoId: Long,
    photoUri: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    sharedElementVisible: Boolean,
) {
    val modifier =
        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier
                    .fillMaxSize()
                    .let { modifier ->
                        if (animatedVisibilityScope != null) {
                            modifier.sharedElement(
                                state = rememberSharedContentState(key = "photo-$photoId"),
                                animatedVisibilityScope = animatedVisibilityScope,
                            )
                        } else {
                            modifier.sharedElementWithCallerManagedVisibility(
                                sharedContentState = rememberSharedContentState(key = "photo-$photoId"),
                                visible = sharedElementVisible,
                            )
                        }
                    }
            }
        } else {
            Modifier.fillMaxSize()
        }

    AsyncImage(
        model =
            ImageRequest.Builder(LocalContext.current)
                .data(photoUri)
                .placeholderMemoryCacheKey("thumb-$photoId")
                .scale(Scale.FIT)
                .crossfade(false)
                .build(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@Suppress("FunctionName")
@Composable
private fun ViewerStateContent(
    title: String,
    message: String,
    actionLabel: String,
    onActionClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = message,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onActionClick) {
            Text(actionLabel)
        }
    }
}
