package com.pixelsea.feature.gallery.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = hiltViewModel(),
    onPhotoClick: (Int) -> Unit // 👇 新增：当照片被点击时触发，传出 index
) {
    val uiState by viewModel.uiState.collectAsState()
    val lazyPagingItems = viewModel.pagedPhotos.collectAsLazyPagingItems()

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_IMAGES
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.handleEvent(GalleryViewEvent.PermissionResult(isGranted))
        }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionToRequest)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PixelSea 相册") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (!uiState.permissionGranted) {
                Text("需要读取存储权限才能显示照片哦", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        count = lazyPagingItems.itemCount,
                        // 1. 为不同类型的 Item 提供独一无二的 Key
                        key = lazyPagingItems.itemKey { item ->
                            when (item) {
                                is GalleryItem.PhotoItem -> "photo_${item.photo.id}"
                                is GalleryItem.HeaderItem -> "header_${item.date}"
                            }
                        },
                        // 2. 动态计算 Span（跨度）
                        span = { index ->
                            val item = lazyPagingItems.peek(index) // peek 不会触发分页加载
                            if (item is GalleryItem.HeaderItem) {
                                GridItemSpan(maxLineSpan) // 标题横跨所有列 (占满整行)
                            } else {
                                GridItemSpan(1) // 照片只占 1 列
                            }
                        }
                    ) { index ->
                        // 3. 根据类型渲染不同的 UI
                        when (val item = lazyPagingItems[index]) {
                            is GalleryItem.HeaderItem -> {
                                Text(
                                    text = item.date,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 16.dp)
                                )
                            }

                            is GalleryItem.PhotoItem -> {
                                // 1. 先计算真实的相片索引（减去前面的标题数量）
                                val headerCount = (0 until index).count { lazyPagingItems.peek(it) is GalleryItem.HeaderItem }
                                val realPhotoIndex = index - headerCount

                                // 2. 完整渲染图片
                                AsyncImage(
                                    model = item.photo.uri,
                                    contentDescription = item.photo.name, // 👈 报错就是因为缺了这个！
                                    contentScale = ContentScale.Crop,     // 👈 填满正方形格子
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .background(Color.LightGray)
                                        .clickable {
                                            // 3. 点击时把正确的 index 传给大图页面
                                            onPhotoClick(realPhotoIndex)
                                        }
                                )
                            }

                            null -> {
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .background(Color.LightGray)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}