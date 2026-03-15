package com.pixelsea.feature.gallery.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Suppress("FunctionName")
data class GalleryPhotoOpenRequest(
    val photoId: Long,
    val photoUri: Uri,
    val boundsInRoot: Rect,
    val photoWidth: Int,
    val photoHeight: Int,
)

@Suppress("FunctionName")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = hiltViewModel(),
    focusPhotoId: Long? = null,
    onPhotoClick: (GalleryPhotoOpenRequest) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedElementVisible: Boolean = true,
) {
    val uiState by viewModel.uiState.collectAsState()
    val galleryItems by viewModel.galleryItems.collectAsState()
    val gridState = rememberLazyGridState()
    var lastHandledFocusPhotoId by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionToRequest =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                val shouldShowRationale =
                    activity?.shouldShowRequestPermissionRationale(permissionToRequest) == true
                viewModel.handleEvent(
                    GalleryViewEvent.PermissionResolved(
                        granted = isGranted,
                        shouldShowRationale = shouldShowRationale,
                    ),
                )
                if (isGranted) {
                    viewModel.refreshIfGranted()
                }
            },
        )

    LaunchedEffect(permissionToRequest, activity) {
        viewModel.syncPermissionState(
            context = context,
            activity = activity,
            permission = permissionToRequest,
        )
        viewModel.refreshIfGranted()
    }

    DisposableEffect(lifecycleOwner, permissionToRequest, activity, context) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.syncPermissionState(
                        context = context,
                        activity = activity,
                        permission = permissionToRequest,
                    )
                    viewModel.refreshIfGranted()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.permissionState) {
        if (uiState.permissionState == GalleryPermissionState.Initial) {
            viewModel.handleEvent(GalleryViewEvent.PermissionRequestLaunched)
            permissionLauncher.launch(permissionToRequest)
        }
    }

    LaunchedEffect(focusPhotoId, galleryItems) {
        val targetPhotoId = focusPhotoId ?: return@LaunchedEffect
        if (targetPhotoId == lastHandledFocusPhotoId) return@LaunchedEffect

        val targetIndex =
            galleryItems.indexOfFirst { item ->
                when (item) {
                    is GalleryItem.PhotoItem -> item.photo.id == targetPhotoId
                    else -> false
                }
            }.takeIf { it >= 0 } ?: return@LaunchedEffect

        val visibleTarget = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
        if (
            visibleTarget != null &&
            isFullyVisible(
                itemInfo = visibleTarget,
                viewportStart = gridState.layoutInfo.viewportStartOffset,
                viewportEnd = gridState.layoutInfo.viewportEndOffset,
            )
        ) {
            lastHandledFocusPhotoId = targetPhotoId
            return@LaunchedEffect
        }

        gridState.scrollToItem(targetIndex)
        withFrameNanos { }

        val centeredTarget = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
        if (centeredTarget != null) {
            val viewportCenter =
                (gridState.layoutInfo.viewportStartOffset + gridState.layoutInfo.viewportEndOffset) / 2
            val itemCenter = centeredTarget.offset.y + (centeredTarget.size.height / 2)
            gridState.scrollBy((itemCenter - viewportCenter).toFloat())
        }

        lastHandledFocusPhotoId = targetPhotoId
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PixelSea 相册") },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState.permissionState) {
                GalleryPermissionState.Granted -> {
                    when {
                        uiState.isLoading && galleryItems.isEmpty() -> {
                            Text("正在加载照片...", style = MaterialTheme.typography.bodyLarge)
                        }

                        uiState.errorMessage != null -> {
                            GalleryStateContent(
                                title = "读取失败",
                                message = uiState.errorMessage ?: "读取相册失败，请重试。",
                                actionLabel = "重试",
                                onActionClick = {
                                    viewModel.handleEvent(GalleryViewEvent.RetryLoadPhotos)
                                },
                            )
                        }

                        uiState.isEmpty -> {
                            GalleryStateContent(
                                title = "还没有可显示的照片",
                                message = "首页目前只显示手机拍摄的照片和截图。你可以先拍一张照片或截一张图再回来查看。",
                                actionLabel = "重新扫描",
                                onActionClick = {
                                    viewModel.handleEvent(GalleryViewEvent.RetryLoadPhotos)
                                },
                            )
                        }

                        else -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 100.dp),
                                    state = gridState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    items(
                                        count = galleryItems.size,
                                        key = { index ->
                                            when (val item = galleryItems[index]) {
                                                is GalleryItem.HeaderItem -> "header_${item.date}_$index"
                                                is GalleryItem.PhotoItem -> "photo_${item.photo.id}"
                                            }
                                        },
                                        span = { index ->
                                            when (galleryItems[index]) {
                                                is GalleryItem.HeaderItem -> GridItemSpan(maxLineSpan)
                                                is GalleryItem.PhotoItem -> GridItemSpan(1)
                                            }
                                        },
                                    ) { index ->
                                        when (val item = galleryItems[index]) {
                                            is GalleryItem.HeaderItem -> {
                                                Text(
                                                    text = item.date,
                                                    style =
                                                        MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                        ),
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 16.dp),
                                                )
                                            }

                                            is GalleryItem.PhotoItem -> {
                                                val thumbnailPlaceholderColor =
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                                var photoBounds by remember(item.photo.id) { mutableStateOf<Rect?>(null) }
                                                val imageModifier =
                                                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                                        with(sharedTransitionScope) {
                                                            Modifier
                                                                .aspectRatio(1f)
                                                                .background(thumbnailPlaceholderColor)
                                                                .onGloballyPositioned { coordinates ->
                                                                    photoBounds = coordinates.boundsInRoot()
                                                                }
                                                                .clickable {
                                                                    val bounds = photoBounds ?: return@clickable
                                                                    onPhotoClick(
                                                                        GalleryPhotoOpenRequest(
                                                                            photoId = item.photo.id,
                                                                            photoUri = Uri.parse(item.photo.uri),
                                                                            boundsInRoot = bounds,
                                                                            photoWidth = item.photo.width,
                                                                            photoHeight = item.photo.height,
                                                                        ),
                                                                    )
                                                                }
                                                                .let { modifier ->
                                                                    if (animatedVisibilityScope != null) {
                                                                        modifier.sharedElement(
                                                                            state =
                                                                                rememberSharedContentState(
                                                                                    key = "photo-${item.photo.id}",
                                                                                ),
                                                                            animatedVisibilityScope = animatedVisibilityScope,
                                                                        )
                                                                    } else {
                                                                        modifier.sharedElementWithCallerManagedVisibility(
                                                                            sharedContentState =
                                                                                rememberSharedContentState(
                                                                                    key = "photo-${item.photo.id}",
                                                                                ),
                                                                            visible = sharedElementVisible,
                                                                        )
                                                                    }
                                                                }
                                                        }
                                                    } else {
                                                        Modifier
                                                            .aspectRatio(1f)
                                                            .background(thumbnailPlaceholderColor)
                                                            .onGloballyPositioned { coordinates ->
                                                                photoBounds = coordinates.boundsInRoot()
                                                            }
                                                            .clickable {
                                                                val bounds = photoBounds ?: return@clickable
                                                                onPhotoClick(
                                                                    GalleryPhotoOpenRequest(
                                                                        photoId = item.photo.id,
                                                                        photoUri = Uri.parse(item.photo.uri),
                                                                        boundsInRoot = bounds,
                                                                        photoWidth = item.photo.width,
                                                                        photoHeight = item.photo.height,
                                                                    ),
                                                                )
                                                            }
                                                    }

                                                AsyncImage(
                                                    model =
                                                        ImageRequest
                                                            .Builder(context)
                                                            .data(item.photo.uri)
                                                            .size(320)
                                                            .memoryCacheKey("thumb-${item.photo.id}")
                                                            .crossfade(false)
                                                            .build(),
                                                    contentDescription = item.photo.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = imageModifier,
                                                )
                                            }
                                        }
                                    }
                                }

                                if (galleryItems.size > 1) {
                                    val quickScrollAnchors =
                                        remember(galleryItems) {
                                            galleryItems.mapIndexedNotNull { index, item ->
                                                when (item) {
                                                    is GalleryItem.HeaderItem -> index
                                                    is GalleryItem.PhotoItem -> null
                                                }
                                            }
                                        }
                                    GalleryQuickScrollBar(
                                        gridState = gridState,
                                        itemCount = galleryItems.size,
                                        anchorIndices = quickScrollAnchors,
                                        labelResolver = { targetIndex ->
                                            resolveNearestHeaderLabel(galleryItems, targetIndex)
                                        },
                                        modifier =
                                            Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 14.dp, top = 20.dp, bottom = 20.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                GalleryPermissionState.Initial -> {
                    Text("正在请求相册权限...", style = MaterialTheme.typography.bodyLarge)
                }

                GalleryPermissionState.Denied -> {
                    GalleryStateContent(
                        title = "需要相册权限",
                        message = "请允许读取图片权限，这样首页才能显示手机拍摄的照片和截图。",
                        actionLabel = "重新授权",
                        onActionClick = {
                            viewModel.handleEvent(GalleryViewEvent.PermissionRequestLaunched)
                            permissionLauncher.launch(permissionToRequest)
                        },
                    )
                }

                GalleryPermissionState.PermanentlyDenied -> {
                    GalleryStateContent(
                        title = "权限已被永久拒绝",
                        message = "当前无法再弹出系统授权框，请前往系统设置手动开启图片读取权限。",
                        actionLabel = "打开设置",
                        onActionClick = { context.openAppSettings() },
                    )
                }
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun GalleryStateContent(
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
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onActionClick) {
            Text(actionLabel)
        }
    }
}

private fun isFullyVisible(
    itemInfo: LazyGridItemInfo,
    viewportStart: Int,
    viewportEnd: Int,
): Boolean {
    val itemTop = itemInfo.offset.y
    val itemBottom = itemTop + itemInfo.size.height
    return itemTop >= viewportStart && itemBottom <= viewportEnd
}

@Suppress("FunctionName")
@Composable
private fun GalleryQuickScrollBar(
    gridState: LazyGridState,
    itemCount: Int,
    anchorIndices: List<Int>,
    labelResolver: (Int) -> String?,
    modifier: Modifier = Modifier,
) {
    var isVisible by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var bubbleLabel by remember { mutableStateOf<String?>(null) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var pendingTargetIndex by remember { mutableIntStateOf(-1) }
    val density = LocalDensity.current
    val alpha by animateFloatAsState(
        targetValue = if (isVisible || isDragging) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "galleryQuickScrollAlpha",
    )

    LaunchedEffect(gridState.isScrollInProgress, isDragging, isVisible) {
        if (gridState.isScrollInProgress || isDragging) {
            isVisible = true
        } else if (isVisible) {
            delay(1200)
            if (!gridState.isScrollInProgress && !isDragging) {
                isVisible = false
            }
        }
    }

    LaunchedEffect(pendingTargetIndex) {
        if (pendingTargetIndex >= 0) {
            withFrameNanos { }
            gridState.scrollToItem(pendingTargetIndex)
        }
    }

    BoxWithConstraints(
        modifier =
            modifier
                .width(220.dp)
                .fillMaxHeight(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        val thumbColor =
            lerp(
                start = MaterialTheme.colorScheme.background,
                stop = MaterialTheme.colorScheme.onBackground,
                fraction = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) 0.14f else 0.18f,
            ).copy(alpha = 0.92f * alpha)

        val bubbleColor =
            lerp(
                start = MaterialTheme.colorScheme.background,
                stop = MaterialTheme.colorScheme.onBackground,
                fraction = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) 0.1f else 0.14f,
            )
        val thumbGlyphColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.32f * alpha)

        val trackHeight = maxHeight
        val bubbleVerticalPadding = 8.dp
        val thumbWidth = 18.dp
        val thumbHeight = thumbWidth * 1.618f
        val availableHeight = (trackHeight - thumbHeight).coerceAtLeast(0.dp)
        val availableHeightPx =
            with(density) {
                availableHeight.toPx()
            }
        val progress =
            if (anchorIndices.isEmpty()) {
                0f
            } else if (isDragging) {
                dragProgress
            } else {
                resolveNearestAnchorProgress(
                    currentIndex = gridState.firstVisibleItemIndex,
                    anchorIndices = anchorIndices,
                )
            }
        val thumbOffset = availableHeight * progress
        val thumbDragState =
            rememberDraggableState { delta ->
                if (availableHeightPx <= 0f) return@rememberDraggableState
                val nextProgress = dragProgress + (delta / availableHeightPx)
                val fraction = nextProgress.coerceIn(0f, 1f)
                dragProgress = fraction
                val targetIndex = resolveAnchorIndexForProgress(fraction, anchorIndices, itemCount)
                bubbleLabel = labelResolver(targetIndex)
                pendingTargetIndex = targetIndex
            }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            AnimatedVisibility(
                visible = isDragging && !bubbleLabel.isNullOrBlank(),
                enter = fadeIn(tween(120)),
                exit = fadeOut(tween(120)),
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            top = (thumbOffset - (thumbHeight * 0.28f)).coerceAtLeast(0.dp),
                            end = thumbWidth * 2f,
                        ),
            ) {
                Text(
                    text = bubbleLabel.orEmpty(),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier =
                        Modifier
                            .defaultMinSize(minWidth = 132.dp)
                            .background(
                                color = bubbleColor,
                                shape = RoundedCornerShape(999.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = bubbleVerticalPadding),
                )
            }

            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = thumbOffset)
                        .width(44.dp)
                        .height(thumbHeight)
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = thumbDragState,
                            startDragImmediately = true,
                            onDragStarted = {
                                isVisible = true
                                isDragging = true
                                dragProgress = progress
                                val targetIndex = resolveAnchorIndexForProgress(progress, anchorIndices, itemCount)
                                bubbleLabel = labelResolver(targetIndex)
                                pendingTargetIndex = targetIndex
                            },
                            onDragStopped = {
                                isDragging = false
                                bubbleLabel = null
                            },
                        ),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(thumbWidth)
                            .height(thumbHeight)
                            .background(
                                color = thumbColor,
                                shape = RoundedCornerShape(999.dp),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = size.width * 0.08f
                        val insetX = size.width * 0.26f
                        val centerX = size.width / 2f

                        val topY = size.height * 0.3f
                        val topPeakY = size.height * 0.22f
                        drawLine(
                            color = thumbGlyphColor,
                            start = androidx.compose.ui.geometry.Offset(insetX, topY),
                            end = androidx.compose.ui.geometry.Offset(centerX, topPeakY),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = thumbGlyphColor,
                            start = androidx.compose.ui.geometry.Offset(centerX, topPeakY),
                            end = androidx.compose.ui.geometry.Offset(size.width - insetX, topY),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )

                        val bottomY = size.height * 0.7f
                        val bottomPeakY = size.height * 0.78f
                        drawLine(
                            color = thumbGlyphColor,
                            start = androidx.compose.ui.geometry.Offset(insetX, bottomY),
                            end = androidx.compose.ui.geometry.Offset(centerX, bottomPeakY),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = thumbGlyphColor,
                            start = androidx.compose.ui.geometry.Offset(centerX, bottomPeakY),
                            end = androidx.compose.ui.geometry.Offset(size.width - insetX, bottomY),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }
    }
}

private fun resolveNearestHeaderLabel(
    galleryItems: List<GalleryItem>,
    index: Int,
): String? {
    if (galleryItems.isEmpty()) return null
    val safeIndex = index.coerceIn(0, galleryItems.lastIndex)
    val headerIndices =
        galleryItems.mapIndexedNotNull { currentIndex, item ->
            if (item is GalleryItem.HeaderItem) currentIndex else null
        }
    if (headerIndices.isEmpty()) return null

    val nearestHeaderIndex =
        headerIndices.minByOrNull { headerIndex ->
            kotlin.math.abs(headerIndex - safeIndex)
        } ?: return null

    val headerItem = galleryItems[nearestHeaderIndex] as? GalleryItem.HeaderItem
    return headerItem?.date
}

private fun resolveNearestAnchorProgress(
    currentIndex: Int,
    anchorIndices: List<Int>,
): Float {
    if (anchorIndices.isEmpty()) return 0f
    if (anchorIndices.size == 1) return 0f

    val nearestAnchorPosition =
        anchorIndices.indices.minByOrNull { anchorPosition ->
            kotlin.math.abs(anchorIndices[anchorPosition] - currentIndex)
        } ?: return 0f

    return nearestAnchorPosition.toFloat() / (anchorIndices.lastIndex).toFloat()
}

private fun resolveAnchorIndexForProgress(
    progress: Float,
    anchorIndices: List<Int>,
    itemCount: Int,
): Int {
    if (anchorIndices.isEmpty()) {
        return progress.times((itemCount - 1).coerceAtLeast(0)).roundToInt()
    }

    val anchorPosition = (progress * anchorIndices.lastIndex).roundToInt().coerceIn(0, anchorIndices.lastIndex)
    return anchorIndices[anchorPosition]
}

private fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun Context.openAppSettings() {
    val intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    startActivity(intent)
}
