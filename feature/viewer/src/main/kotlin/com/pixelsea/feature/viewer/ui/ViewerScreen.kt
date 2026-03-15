package com.pixelsea.feature.viewer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.pixelsea.core.data.model.Photo
import kotlinx.coroutines.launch

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
) {
    val photos by viewModel.photos.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { photos.size.coerceAtLeast(1) })
    val coroutineScope = rememberCoroutineScope()

    var currentIndex by remember { mutableStateOf(0) }
    var isFilmstripVisible by remember { mutableStateOf(true) }

    LaunchedEffect(initialPhotoId) {
        isFilmstripVisible = true
    }

    LaunchedEffect(photos, initialPhotoId, pagerState) {
        if (photos.isNotEmpty()) {
            currentIndex = photos.indexOfFirst { it.id == initialPhotoId }.takeIf { it >= 0 } ?: 0
            pagerState.scrollToPage(currentIndex)
        }
    }

    LaunchedEffect(pagerState, photos) {
        if (photos.isEmpty()) return@LaunchedEffect

        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                currentIndex = page.coerceIn(0, photos.lastIndex)
            }
    }

    BackHandler(enabled = true) {
        val currentPhotoId =
            photos
                .getOrNull(currentIndex.coerceAtLeast(0))
                ?.id
                ?: initialPhotoId
        onBackClick(currentPhotoId)
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
                )
            }

            photos.isNotEmpty() -> {
                var scale by remember(currentIndex) { mutableFloatStateOf(1f) }
                var offset by remember(currentIndex) { mutableStateOf(Offset.Zero) }
                var viewportSize by remember(currentIndex) { mutableStateOf(Size.Zero) }
                val bottomChromeHeight = 136.dp

                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = scale <= 1f,
                        beyondViewportPageCount = 1,
                    ) { page ->
                        val photo = photos[page]
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
                                            .pointerInput(photo.id) {
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
                                                    onTap = {
                                                        isFilmstripVisible = !isFilmstripVisible
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
                                                modifier.sharedElement(
                                                    state = rememberSharedContentState(key = "photo-${photo.id}"),
                                                    animatedVisibilityScope = animatedVisibilityScope,
                                                )
                                            }
                                    }
                                } else {
                                    Modifier
                                        .fillMaxSize()
                                        .onSizeChanged { size ->
                                            viewportSize = Size(size.width.toFloat(), size.height.toFloat())
                                        }
                                        .pointerInput(photo.id) {
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
                                                onTap = {
                                                    isFilmstripVisible = !isFilmstripVisible
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

                    AnimatedVisibility(
                        visible = isFilmstripVisible,
                        enter = fadeIn(animationSpec = tween(durationMillis = 120)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 90)),
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(bottomChromeHeight)
                                    .background(Color.Black),
                        ) {
                            ViewerFilmstrip(
                                photos = photos,
                                currentIndex = currentIndex,
                                onCurrentIndexChange = { targetIndex ->
                                    currentIndex = targetIndex
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(targetIndex)
                                    }
                                },
                                modifier =
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerFilmstrip(
    photos: List<Photo>,
    currentIndex: Int,
    onCurrentIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentIndexState = rememberUpdatedState(currentIndex)
    val visibleIndices =
        remember(photos, currentIndex) {
            (-6..6).map { offset ->
                val targetIndex = currentIndex + offset
                targetIndex.takeIf { it in photos.indices }
            }
        }

    BoxWithConstraints(
        modifier =
            modifier
                .background(Color.Black)
                .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        val slotSpacing = 5.dp
        val totalSpacing = slotSpacing * 12
        val sideThumbnailAspectRatio = 10f / 16f
        val sideSlotWeight = sideThumbnailAspectRatio
        val selectedSlotWeight = 1f
        val baseThumbnailHeight =
            calculateFilmstripBaseHeight(
                maxWidth = maxWidth,
                totalSpacing = totalSpacing,
                totalWidthWeight = (sideSlotWeight * 12) + selectedSlotWeight,
            ).coerceAtMost(52.dp)
        val sideThumbnailWidth = baseThumbnailHeight * sideThumbnailAspectRatio
        val selectedThumbnailWidth = baseThumbnailHeight
        val centerStepDistance = ((selectedThumbnailWidth + sideThumbnailWidth) / 2) + slotSpacing
        val dragSwitchDistance = centerStepDistance * 0.72f
        val thumbnailShape = RoundedCornerShape(4.dp)
        val filmstripHeight = baseThumbnailHeight
        var dragOffset by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }
        val displayedDragOffset by
            animateFloatAsState(
                targetValue = if (isDragging) dragOffset else 0f,
                animationSpec =
                    if (isDragging) {
                        tween(durationMillis = 0)
                    } else {
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        )
                    },
                label = "filmstripDragOffset",
            )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(filmstripHeight),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer(
                            translationX = displayedDragOffset,
                        )
                        .pointerInput(photos) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                },
                                onDrag = { change, dragAmount ->
                                    if (kotlin.math.abs(dragAmount.x) <= kotlin.math.abs(dragAmount.y)) {
                                        return@detectDragGestures
                                    }

                                    change.consume()
                                    dragOffset += dragAmount.x

                                    var targetIndex = currentIndexState.value
                                    val stepPx = dragSwitchDistance.toPx()

                                    while (dragOffset >= stepPx && targetIndex > 0) {
                                        dragOffset -= stepPx
                                        targetIndex--
                                        onCurrentIndexChange(targetIndex)
                                    }
                                    while (dragOffset <= -stepPx && targetIndex < photos.lastIndex) {
                                        dragOffset += stepPx
                                        targetIndex++
                                        onCurrentIndexChange(targetIndex)
                                    }

                                    if ((targetIndex == 0 && dragOffset > 0f) ||
                                        (targetIndex == photos.lastIndex && dragOffset < 0f)
                                    ) {
                                        dragOffset *= 0.28f
                                    }
                                },
                                onDragEnd = {
                                    isDragging = false
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    isDragging = false
                                    dragOffset = 0f
                                },
                            )
                        },
                horizontalArrangement = Arrangement.spacedBy(slotSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                visibleIndices.forEachIndexed { slotIndex, photoIndex ->
                    val isCenterSlot = slotIndex == 6
                    val itemWidth = if (isCenterSlot) selectedThumbnailWidth else sideThumbnailWidth
                    val itemAspectRatio = if (isCenterSlot) 1f else sideThumbnailAspectRatio

                    if (photoIndex == null) {
                        Spacer(
                            modifier =
                                Modifier
                                    .width(itemWidth)
                                    .aspectRatio(itemAspectRatio),
                        )
                    } else {
                        val photo = photos[photoIndex]
                        val isSelected = photoIndex == currentIndex
                        val overlayAlpha by
                            animateFloatAsState(
                                targetValue = if (isSelected) 0f else 0.18f,
                                animationSpec = tween(durationMillis = 140),
                                label = "filmstripOverlay-${photo.id}",
                            )

                        Box(
                            modifier =
                                Modifier
                                    .width(itemWidth)
                                    .aspectRatio(itemAspectRatio)
                                    .background(
                                        color = Color.White.copy(alpha = if (isSelected) 0.06f else 0.02f),
                                        shape = thumbnailShape,
                                    )
                                    .clickable(enabled = !isSelected) {
                                        isDragging = false
                                        dragOffset = 0f
                                        onCurrentIndexChange(photoIndex)
                                    },
                        ) {
                            AsyncImage(
                                model =
                                    ImageRequest.Builder(LocalContext.current)
                                        .data(photo.uri)
                                        .placeholderMemoryCacheKey("thumb-${photo.id}")
                                        .scale(Scale.FILL)
                                        .crossfade(false)
                                        .build(),
                                contentDescription = photo.name,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(thumbnailShape),
                            )
                            if (overlayAlpha > 0f) {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = overlayAlpha)),
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}

private fun calculateFilmstripBaseHeight(
    maxWidth: Dp,
    totalSpacing: Dp,
    totalWidthWeight: Float,
): Dp {
    return (maxWidth - totalSpacing) / totalWidthWeight
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ViewerOpeningPhoto(
    photoId: Long,
    photoUri: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val modifier =
        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier
                    .fillMaxSize()
                    .let { targetModifier ->
                        targetModifier.sharedElement(
                            state = rememberSharedContentState(key = "photo-$photoId"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
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
