/**
 * PixelSea 应用模块构建配置文件
 *
 * 功能说明:
 * - 配置 Android 应用插件和编译选项
 * - 定义应用版本、SDK 版本等基本信息
 * - 管理项目依赖库
 */
plugins {
    // Android 应用基础插件
    id("com.android.application")
    // Kotlin Android 支持插件
    id("org.jetbrains.kotlin.android")
    // PixelSea 自定义 Compose 配置插件
    id("pixelsea.android.compose")
    /**
     * Hilt 依赖注入插件
     * 为 App 模块提供依赖注入能力，支持 Dagger-Hilt 框架
     */
    id("pixelsea.android.hilt")
}

/**
 * Android 应用配置块
 *
 * 包含应用的编译配置、版本信息、构建类型等设置
 */
android {
    // 应用的包命名空间，用于资源访问
    namespace = "com.example.pixelsea"
    // 编译 SDK 版本号
    compileSdk = 36

    /**
     * 默认配置块
     *
     * 属性说明:
     * - applicationId: 应用的唯一标识符
     * - minSdk: 最低支持的 Android API 级别 (29 = Android 10)
     * - targetSdk: 目标 Android API 级别 (36 = Android 15)
     * - versionCode: 内部版本号，用于版本升级控制
     * - versionName: 对外展示的版本名称
     * - testInstrumentationRunner: 测试运行器类名
     */
    defaultConfig {
        applicationId = "com.example.pixelsea"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /**
     * 构建类型配置
     *
     * release: 发布版本配置
     * - isMinifyEnabled: 是否启用代码混淆优化 (false=不启用)
     * - proguardFiles: ProGuard 规则文件配置，用于代码压缩和优化
     */
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    /**
     * Java 编译选项配置
     *
     * 指定 Java 源代码和目标字节码的兼容性版本为 Java 17
     */
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    /**
     * Kotlin 编译选项配置
     *
     * jvmTarget: Kotlin 编译器的 JVM 目标版本，与 Java 版本保持一致
     */
    kotlinOptions {
        jvmTarget = "17"
    }
}

/**
 * 依赖管理配置块
 *
 * 定义应用所需的所有外部库和本地模块依赖
 */
dependencies {

    // ========== Android 核心库 ==========
    // Kotlin 扩展函数库，提供便捷的 API
    implementation("androidx.core:core-ktx:1.17.0")
    // Lifecycle 运行时库，支持生命周期感知组件
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    // Activity Compose 集成库，支持 Compose 与 Activity 交互
    implementation("androidx.activity:activity-compose:1.12.4")

    // ========== Jetpack Compose UI 库 ==========
    // Compose BOM (Bill of Materials)，统一管理 Compose 库版本
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    // Compose UI 基础组件库
    implementation("androidx.compose.ui:ui")
    // Compose 图形渲染库，支持绘图和图像处理
    implementation("androidx.compose.ui:ui-graphics")
    // Compose 预览工具库，支持@Preview 注解
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Material Design 3 组件库
    implementation("androidx.compose.material3:material3")
    // Coil Compose 图片加载库
    implementation("io.coil-kt:coil-compose:2.5.0")

    // ========== 测试库 ==========
    // JUnit4 单元测试框架
    testImplementation("junit:junit:4.13.2")
    // AndroidX Test JUnit 扩展库
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    // Espresso UI 测试框架核心库
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    // Compose UI 测试库的 BOM 版本管理
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    // Compose UI 测试 JUnit4 集成库
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    // Compose UI 调试工具库 (仅 debug 模式)
    debugImplementation("androidx.compose.ui:ui-tooling")
    // Compose UI 测试清单库 (仅 debug 模式)
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ========== 本地业务模块依赖 ==========
    // 相册功能模块
    // 提供图片浏览、选择等相册相关功能
    implementation(project(":feature:gallery"))

    // 图片预览功能模块
    // 提供图片查看、缩放、旋转等预览功能
    implementation(project(":feature:viewer"))

    // ========== 导航组件 ==========

    // Navigation Compose 库
    // 提供基于 Compose 的导航和路由管理功能
    implementation("androidx.navigation:navigation-compose:2.7.7")
}
