package com.pixelsea.feature.viewer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale

/**
 * 照片大图预览页面
 * 
 * 功能说明：
 * - 全屏显示单张照片
 * - 支持左右滑动切换照片
 * - 支持双击放大/缩小
 * - 放大状态下支持拖拽查看细节
 * - 单击返回相册页面
 * 
 * @param initialPhotoId 初始显示的照片 ID，用于定位到正确的照片
 * @param onBackClick 返回按钮回调
 * @param viewModel 视图模型，通过 Hilt 自动注入
 */
@Composable
fun ViewerScreen(
    initialPhotoId: Long,
    onBackClick: () -> Unit,
    viewModel: ViewerViewModel = hiltViewModel()
) {
    // 获取分页照片数据
    val lazyPagingItems = viewModel.pagedPhotos.collectAsLazyPagingItems()
    
    // 计算屏幕宽度和滑动阈值
    val density = LocalDensity.current
    val screenWidth = with(density) { 1.dp.toPx() * 360 }  // 假设屏幕宽度为 360dp
    val swipeThreshold = screenWidth * 0.15f               // 滑动阈值为屏幕宽度的 15%

    // 当前显示的照片索引
    var currentIndex by remember { mutableStateOf(0) }
    // 是否已完成初始化（找到目标照片）
    var isInitialized by remember { mutableStateOf(false) }
    
    // 初始化：根据 photoId 查找对应的照片索引
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
            // 如果没找到，默认显示第一张
            if (!isInitialized && lazyPagingItems.itemSnapshotList.items.isNotEmpty()) {
                currentIndex = 0
                isInitialized = true
            }
        }
    }

    // 主容器：黑色背景，居中显示
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (lazyPagingItems.itemSnapshotList.items.isNotEmpty()) {
            // 滑动偏移量
            var dragOffset by remember { mutableFloatStateOf(0f) }
            // 是否正在拖拽
            var isDragging by remember { mutableStateOf(false) }
            // 滑动方向（用于动画）
            var isSwipingLeft by remember { mutableStateOf(false) }
            // 获取数据快照
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
                                // 累积滑动偏移
                                dragOffset += dragAmount.x
                            },
                            onDragEnd = {
                                isDragging = false
                                // 检测滑动方向，每次只切换一张
                                if (dragOffset > swipeThreshold && currentIndex > 0) {
                                    // 向左滑动（手指向右拖），切换到上一张
                                    isSwipingLeft = true
                                    currentIndex--
                                } else if (dragOffset < -swipeThreshold && currentIndex < items.size - 1) {
                                    // 向右滑动（手指向左拖），切换到下一张
                                    isSwipingLeft = false
                                    currentIndex++
                                }
                                // 重置滑动偏移
                                dragOffset = 0f
                            }
                        )
                    }
            ) {
                // 带动画的内容切换
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
                    
                    // 缩放和偏移状态（用于双击放大功能）
                    var scale by remember { mutableFloatStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photo.uri)
                            .scale(Scale.FIT)  // 适应屏幕，保持比例
                            .build(),
                        contentDescription = photo.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            // 单击和双击手势
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1f) {
                                            // 已放大状态，双击恢复原状
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            // 原始状态，双击放大 2.5 倍
                                            scale = 2.5f
                                        }
                                    },
                                    onTap = { onBackClick() }  // 单击返回
                                )
                            }
                            // 放大状态下的拖拽手势
                            .pointerInput(scale) {
                                if (scale > 1f) {
                                    detectDragGestures { change, dragAmount ->
                                        if (!isDragging) {
                                            change.consume()
                                            // 计算最大偏移量，防止拖出边界
                                            val maxOffsetX = (size.width * (scale - 1)) / 2
                                            val maxOffsetY = (size.height * (scale - 1)) / 2
                                            // 更新偏移，限制在最大范围内
                                            offset = Offset(
                                                x = (offset.x + dragAmount.x).coerceIn(-maxOffsetX, maxOffsetX),
                                                y = (offset.y + dragAmount.y).coerceIn(-maxOffsetY, maxOffsetY)
                                            )
                                        }
                                    }
                                }
                            }
                            // 应用缩放和偏移变换
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
            // 数据加载中显示进度指示器
            CircularProgressIndicator(color = Color.White)
        }
    }
}