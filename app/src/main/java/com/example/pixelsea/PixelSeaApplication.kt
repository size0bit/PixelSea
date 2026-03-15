package com.example.pixelsea

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用的 Application 类
 * 使用 @HiltAndroidApp 注解，启用 Hilt 依赖注入框架
 * 所有依赖注入的起点，管理全局的单例对象
 */
@HiltAndroidApp
class PixelSeaApplication : Application()
