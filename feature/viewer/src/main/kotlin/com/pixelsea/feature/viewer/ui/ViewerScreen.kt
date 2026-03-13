package com.pixelsea.feature.viewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage

@Composable
fun ViewerScreen(
    initialIndex: Int,
    onBackClick: () -> Unit,
    viewModel: ViewerViewModel = hiltViewModel()
) {
    val lazyPagingItems = viewModel.pagedPhotos.collectAsLazyPagingItems()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (lazyPagingItems.itemCount > 0) {
            val safeInitialPage = initialIndex.coerceIn(0, lazyPagingItems.itemCount - 1)
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitialPage)

            // 👇 终极魔法 1：纯逻辑写出的“松手自动吸附翻页”
            // 我们监听列表的滚动状态，当你的手指离开屏幕、滚动停止时，它会自动计算距离并吸附
            val isScrollInProgress = listState.isScrollInProgress
            LaunchedEffect(isScrollInProgress) {
                if (!isScrollInProgress) {
                    val index = listState.firstVisibleItemIndex
                    val offset = listState.firstVisibleItemScrollOffset
                    val itemSize = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1

                    if (offset > 0) {
                        if (offset > itemSize / 2) {
                            // 如果滑过去超过了一半，就自动吸附到下一张图
                            listState.animateScrollToItem(index + 1)
                        } else {
                            // 如果滑了不到一半就松手，就自动弹回当前这张图
                            listState.animateScrollToItem(index)
                        }
                    }
                }
            }

            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    count = lazyPagingItems.itemCount
                ) { page ->
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val photo = lazyPagingItems[page]
                        if (photo != null) {
                            var scale by remember { mutableFloatStateOf(1f) }
                            var offset by remember { mutableStateOf(Offset.Zero) }

                            AsyncImage(
                                model = photo.uri,
                                contentDescription = photo.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    // 👇 终极魔法 2：双击放大与单击返回
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                if (scale > 1f) {
                                                    scale = 1f // 如果已经放大，双击恢复原状
                                                    offset = Offset.Zero
                                                } else {
                                                    scale = 2.5f // 如果是原图，双击放大 2.5 倍
                                                }
                                            },
                                            onTap = { onBackClick() } // 单击退出全屏
                                        )
                                    }
                                    // 👇 终极魔法 3：只有在放大状态下，才允许单指拖拽图片
                                    .pointerInput(scale) {
                                        if (scale > 1f) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume() // 拦截滑动事件，不让 LazyRow 翻页
                                                val maxOffsetX = (size.width * (scale - 1)) / 2
                                                val maxOffsetY = (size.height * (scale - 1)) / 2
                                                offset = Offset(
                                                    x = (offset.x + dragAmount.x).coerceIn(-maxOffsetX, maxOffsetX),
                                                    y = (offset.y + dragAmount.y).coerceIn(-maxOffsetY, maxOffsetY)
                                                )
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
                        } else {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }
            }
        } else {
            CircularProgressIndicator(color = Color.White)
        }
    }
}