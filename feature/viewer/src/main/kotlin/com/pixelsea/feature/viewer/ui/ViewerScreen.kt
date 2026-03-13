package com.pixelsea.feature.viewer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ViewerScreen(
    initialPhotoId: Long,
    onBackClick: () -> Unit,
    viewModel: ViewerViewModel = hiltViewModel()
) {
    val lazyPagingItems = viewModel.pagedPhotos.collectAsLazyPagingItems()
    val density = LocalDensity.current
    val screenWidth = with(density) { 1.dp.toPx() * 360 }
    val swipeThreshold = screenWidth * 0.15f

    var currentIndex by remember { mutableStateOf(0) }
    var isInitialized by remember { mutableStateOf(false) }
    
    // 使用 itemSnapshotList.items 确保与 GalleryScreen 数据一致
    LaunchedEffect(lazyPagingItems.itemSnapshotList.items.size, initialPhotoId) {
        if (lazyPagingItems.itemSnapshotList.items.isNotEmpty() && !isInitialized) {
            // 遍历所有已加载的项，查找匹配的 photoId
            for (i in lazyPagingItems.itemSnapshotList.items.indices) {
                val photo = lazyPagingItems.itemSnapshotList.items[i]
                if (photo.id == initialPhotoId) {
                    currentIndex = i
                    isInitialized = true
                    break
                }
            }
            // 如果还没找到，使用第一个
            if (!isInitialized && lazyPagingItems.itemSnapshotList.items.isNotEmpty()) {
                currentIndex = 0
                isInitialized = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (lazyPagingItems.itemSnapshotList.items.isNotEmpty()) {
            var dragOffset by remember { mutableFloatStateOf(0f) }
            var isDragging by remember { mutableStateOf(false) }
            var isSwipingLeft by remember { mutableStateOf(false) }
            val items = lazyPagingItems.itemSnapshotList.items

            // 自定义滑动逻辑，每次只切换一张图片
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount.x
                            },
                            onDragEnd = {
                                isDragging = false
                                // 检测滑动方向，每次只切换一张
                                if (dragOffset > swipeThreshold && currentIndex > 0) {
                                    // 向左滑动，切换到上一张
                                    isSwipingLeft = true
                                    currentIndex--
                                } else if (dragOffset < -swipeThreshold && currentIndex < items.size - 1) {
                                    // 向右滑动，切换到下一张
                                    isSwipingLeft = false
                                    currentIndex++
                                }
                                // 重置滑动偏移
                                dragOffset = 0f
                            }
                        )
                    }
            ) {
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        if (isSwipingLeft) {
                            // 向左滑动，新图片从左侧滑入，旧图片从右侧滑出
                            slideInHorizontally(tween(300)) { -it } togetherWith
                                slideOutHorizontally(tween(300)) { it }
                        } else {
                            // 向右滑动，新图片从右侧滑入，旧图片从左侧滑出
                            slideInHorizontally(tween(300)) { it } togetherWith
                                slideOutHorizontally(tween(300)) { -it }
                        }
                    }
                ) { index ->
                    // 确保索引在有效范围内
                    val safeIndex = index.coerceIn(0, items.size - 1)
                    val photo = items[safeIndex]
                    var scale by remember { mutableFloatStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photo.uri)
                            .scale(Scale.FIT)
                            .build(),
                        contentDescription = photo.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                        }
                                    },
                                    onTap = { onBackClick() }
                                )
                            }
                            .pointerInput(scale) {
                                if (scale > 1f) {
                                    detectDragGestures {
                                        change, dragAmount ->
                                        if (!isDragging) {
                                            change.consume()
                                            val maxOffsetX = (size.width * (scale - 1)) / 2
                                            val maxOffsetY = (size.height * (scale - 1)) / 2
                                            offset = Offset(
                                                x = (offset.x + dragAmount.x).coerceIn(-maxOffsetX, maxOffsetX),
                                                y = (offset.y + dragAmount.y).coerceIn(-maxOffsetY, maxOffsetY)
                                            )
                                        }
                                    }
                                }
                            }
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }
            }
        } else {
            CircularProgressIndicator(color = Color.White)
        }
    }
}