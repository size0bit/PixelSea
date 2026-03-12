package com.example.pixelsea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.pixelsea.ui.theme.PixelSeaTheme // 这是 AS 默认生成的主题，保留它
import com.pixelsea.feature.gallery.ui.GalleryScreen // 导入我们刚刚写的时光轴 UI
import dagger.hilt.android.AndroidEntryPoint

/**
 * 应用的主入口 Activity
 * 使用 Hilt 进行依赖注入，管理整个应用的生命周期
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置 Compose 作为 UI 渲染引擎，替代传统的 XML 布局
        setContent {
            // 使用默认的 Material 3 主题包裹我们的 UI
            PixelSeaTheme {
                // 正式挂载相册时光轴页面！
                GalleryScreen()
            }
        }
    }
}