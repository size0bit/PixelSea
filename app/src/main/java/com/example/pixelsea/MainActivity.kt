package com.example.pixelsea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pixelsea.ui.theme.PixelSeaTheme
import com.pixelsea.feature.gallery.ui.GalleryScreen
import com.pixelsea.feature.viewer.ui.ViewerScreen

import dagger.hilt.android.AndroidEntryPoint

/**
 * 应用主入口 Activity
 * 
 * 职责说明：
 * - 作为应用的单一入口点，使用单 Activity 架构
 * - 配置 Hilt 依赖注入
 * - 设置导航图，管理页面跳转
 * - 应用全局主题
 * 
 * 导航结构：
 * - gallery: 相册网格页面（起始页）
 * - viewer/{photoId}: 大图预览页面，接收照片 ID 参数
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    /**
     * Activity 创建时的初始化
     * 
     * @param savedInstanceState 保存的实例状态，用于配置更改后恢复
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 应用全局主题
            PixelSeaTheme {
                // 根容器，设置背景色
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 创建导航控制器，管理页面栈
                    val navController = rememberNavController()
                    
                    // 刷新键：用于强制 GalleryScreen 刷新数据
                    // 每次从 Viewer 返回时更新此值，触发 Gallery 重新加载
                    var galleryRefreshKey by remember { mutableStateOf(0L) }

                    // 导航宿主，定义页面路由图
                    NavHost(navController = navController, startDestination = "gallery") {

                        // 路由：相册网格页面
                        composable("gallery") {
                            GalleryScreen(
                                refreshKey = galleryRefreshKey,  // 传递刷新键
                                onPhotoClick = { photoId ->
                                    // 点击照片时，携带 photoId 导航到大图预览
                                    navController.navigate("viewer/$photoId")
                                }
                            )
                        }

                        // 路由：大图预览页面
                        // 使用路径参数传递照片 ID
                        composable(
                            route = "viewer/{photoId}",
                            arguments = listOf(navArgument("photoId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            // 从路由参数中提取照片 ID
                            val photoId = backStackEntry.arguments?.getLong("photoId") ?: 0L

                            ViewerScreen(
                                initialPhotoId = photoId,  // 初始显示的照片 ID
                                onBackClick = {
                                    // 返回相册页面
                                    navController.popBackStack()
                                    // 更新刷新键，强制 Gallery 重新加载数据
                                    // 使用当前时间戳确保值每次都不同
                                    galleryRefreshKey = System.currentTimeMillis()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}