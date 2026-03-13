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
                    val navController = rememberNavController()
                    
                    // 使用计数器强制每次返回时刷新 Gallery
                    var galleryRefreshKey by remember { mutableStateOf(0L) }

                    NavHost(navController = navController, startDestination = "gallery") {

                        composable("gallery") {
                            GalleryScreen(
                                refreshKey = galleryRefreshKey,
                                onPhotoClick = { photoId ->
                                    navController.navigate("viewer/$photoId")
                                }
                            )
                        }

                        composable(
                            route = "viewer/{photoId}",
                            arguments = listOf(navArgument("photoId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val photoId = backStackEntry.arguments?.getLong("photoId") ?: 0L

                            ViewerScreen(
                                initialPhotoId = photoId,
                                onBackClick = {
                                    navController.popBackStack()
                                    // 返回时更新 key，强制 GalleryScreen 刷新
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