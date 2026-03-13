package com.example.pixelsea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pixelsea.ui.theme.PixelSeaTheme
import com.pixelsea.feature.gallery.ui.GalleryScreen
import com.pixelsea.feature.viewer.ui.ViewerScreen // 导入 Viewer

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelSeaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 1. 创建导航控制器（遥控器）
                    val navController = rememberNavController()

                    // 2. 搭建路由地图，设置起始页为 "gallery"
                    NavHost(navController = navController, startDestination = "gallery") {

                        // 目的地 A：相册网格页
                        composable("gallery") {
                            GalleryScreen(
                                onPhotoClick = { clickedIndex ->
                                    // 发生点击时，带着 index 跳转到 viewer
                                    navController.navigate("viewer/$clickedIndex")
                                }
                            )
                        }

                        // 目的地 B：大图预览页（声明需要接收一个叫 index 的 Int 类型参数）
                        composable(
                            route = "viewer/{index}",
                            arguments = listOf(navArgument("index") { type = NavType.IntType })
                        ) { backStackEntry ->
                            // 从路由中提取出传过来的 index
                            val index = backStackEntry.arguments?.getInt("index") ?: 0

                            ViewerScreen(
                                initialIndex = index,
                                onBackClick = {
                                    // 点击返回时，弹出当前页面，回到上一页
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}