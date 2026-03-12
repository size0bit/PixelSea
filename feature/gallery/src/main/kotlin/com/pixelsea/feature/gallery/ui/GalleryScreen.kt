package com.pixelsea.feature.gallery.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * 相册浏览页面主入口
 * 使用 MVI 架构模式，通过 ViewModel 管理状态和事件
 * @param viewModel 依赖注入的 ViewModel，自动从 Hilt 获取
 */
fun GalleryScreen(
    viewModel: GalleryViewModel = hiltViewModel() // Hilt 自动注入 ViewModel
) {
    // 观察 ViewModel 里的状态
    // collectAsState 将 StateFlow 转换为 Compose 的 State，实现响应式更新
    val uiState by viewModel.uiState.collectAsState()

    // 适配 Android 13 分区存储的权限策略
    // Android 13 (API 33) 及以上使用 READ_MEDIA_IMAGES，以下使用 READ_EXTERNAL_STORAGE
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_IMAGES
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // Compose 原生的权限请求启动器
    // 使用 ActivityResultContracts.RequestPermission 处理运行时权限申请
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            // 把用户的授权结果作为 Event 丢给 ViewModel
            viewModel.handleEvent(GalleryViewEvent.PermissionResult(isGranted))
        }
    )

    // 页面第一次渲染时，主动请求权限
    // LaunchedEffect 确保只在首次组合时执行一次
    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionToRequest)
    }

    // Scaffold 提供标准的 Material Design 布局结构
    Scaffold(
        topBar = {
            // 顶部应用栏，显示标题
            TopAppBar(
                title = { Text("PixelSea") },
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
            // 根据 UI 状态展示不同的内容
            when {
                // 1. 正在加载 - 显示进度条
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }
                // 2. 拒绝了权限 - 提示用户需要权限
                !uiState.permissionGranted -> {
                    Text("需要读取存储权限才能显示照片哦", style = MaterialTheme.typography.bodyLarge)
                }
                // 3. 权限通过，但相册为空
                uiState.photos.isEmpty() -> {
                    Text("相册空空如也", style = MaterialTheme.typography.bodyLarge)
                }
                // 4. 万事俱备，展示照片！
                else -> {
                    // 使用 LazyVerticalGrid 构建自适应网格布局
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(
                            items = uiState.photos,
                            key = { photo -> photo.id } // 提供 key 能大幅提升滑动性能
                        ) { photo ->
                            // 使用 Coil 异步加载真实图片
                            // AsyncImage 会自动处理图片的下载、缓存和显示
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = photo.name,
                                contentScale = ContentScale.Crop, // 自动裁剪填充正方形
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .background(Color.LightGray) // 加载出来前的占位底色
                            )
                        }
                    }
                }
            }
        }
    }
}