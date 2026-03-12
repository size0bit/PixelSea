package com.example.pixelsea.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * 暗色主题配色方案
 * 使用亮色系的数值（80 系列）在暗色背景下提供更好的对比度
 */
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,      // 主色调 - 用于按钮、选中状态等
    secondary = PurpleGrey80, // 次要色 - 用于辅助元素
    tertiary = Pink80         // 第三色 - 用于装饰性元素
)

/**
 * 亮色主题配色方案
 * 使用深色系的数值（40 系列）在亮色背景下提供更好的可读性
 */
private val LightColorScheme = lightColorScheme(
    primary = Purple40,      // 主色调 - 用于按钮、选中状态等
    secondary = PurpleGrey40, // 次要色 - 用于辅助元素
    tertiary = Pink40         // 第三色 - 用于装饰性元素

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
/**
 * PixelSea 应用主题包装器
 * 提供 Material 3 主题支持，包含颜色方案和排版样式
 * @param darkTheme 是否使用暗色主题，默认跟随系统设置
 * @param dynamicColor 是否启用动态配色（Android 12+），根据用户壁纸生成主题色
 * @param content 主题包裹的内容
 */
fun PixelSeaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // 根据条件选择合适的配色方案
    val colorScheme = when {
        // Android 12+ 支持动态配色，根据用户壁纸生成主题色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        // 否则使用预设的暗色或亮色主题
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 使用 MaterialTheme 包裹内容，应用配色方案和排版样式
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}