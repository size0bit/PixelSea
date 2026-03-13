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
import coil.compose.AsyncImage

/**
 * 相册网格页面
 * 
 * 功能说明：
 * - 以网格形式展示设备中的所有照片
 * - 支持时间轴分组，按日期显示照片
 * - 点击照片可进入大图预览模式
 * - 自动请求存储权限
 * 
 * @param viewModel 相册视图模型，通过 Hilt 自动注入
 * @param onPhotoClick 照片点击回调，传递照片 ID 用于导航到大图预览
 * @param refreshKey 刷新键，当值改变时触发数据刷新
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = hiltViewModel(),
    onPhotoClick: (Long) -> Unit,
    refreshKey: Any? = null
) {
    // 收集 UI 状态，实现响应式更新
    val uiState by viewModel.uiState.collectAsState()
    // 将分页数据转换为 Compose 可用的 LazyPagingItems
    val lazyPagingItems = viewModel.pagedPhotos.collectAsLazyPagingItems()

    // 当 refreshKey 改变时，刷新数据
    LaunchedEffect(refreshKey) {
        lazyPagingItems.refresh()
    }

    // 根据系统版本选择合适的权限
    // Android 13 (TIRAMISU) 及以上使用 READ_MEDIA_IMAGES
    // 低版本使用 READ_EXTERNAL_STORAGE
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_IMAGES
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // 创建权限请求启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            // 将权限结果传递给 ViewModel 处理
            viewModel.handleEvent(GalleryViewEvent.PermissionResult(isGranted))
        }
    )

    // 组件首次加载时请求权限
    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionToRequest)
    }

    // 页面骨架结构
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
                // 权限未授予时显示提示信息
                Text("需要读取存储权限才能显示照片哦", style = MaterialTheme.typography.bodyLarge)
            } else {
                // 照片网格列表
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp), // 自适应列数，最小宽度 100dp
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp), // 水平间距
                    verticalArrangement = Arrangement.spacedBy(2.dp)    // 垂直间距
                ) {
                    // 获取已加载的数据快照，确保数据一致性
                    val items = lazyPagingItems.itemSnapshotList.items
                    
                    items(
                        count = items.size,
                        key = { index ->
                            // 为每个 item 提供唯一 key，优化列表性能
                            when (val item = items[index]) {
                                is GalleryItem.HeaderItem -> "header_${item.date}"
                                is GalleryItem.PhotoItem -> "photo_${item.photo.id}"
                            }
                        },
                        span = { index ->
                            // 控制每个 item 占用的网格跨度
                            // HeaderItem 横跨整行，PhotoItem 只占一个格子
                            when (items[index]) {
                                is GalleryItem.HeaderItem -> GridItemSpan(maxLineSpan)
                                is GalleryItem.PhotoItem -> GridItemSpan(1)
                            }
                        }
                    ) { index ->
                        when (val item = items[index]) {
                            // 时间轴日期标题
                            is GalleryItem.HeaderItem -> {
                                Text(
                                    text = item.date,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 16.dp)
                                )
                            }
                            // 照片预览项
                            is GalleryItem.PhotoItem -> {
                                AsyncImage(
                                    model = item.photo.uri,           // 图片 URI
                                    contentDescription = item.photo.name,
                                    contentScale = ContentScale.Crop, // 裁剪填充
                                    modifier = Modifier
                                        .aspectRatio(1f)             // 1:1 正方形
                                        .background(Color.LightGray)  // 加载中背景色
                                        .clickable {
                                            // 点击时传递照片 ID 给外部处理
                                            onPhotoClick(item.photo.id)
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}