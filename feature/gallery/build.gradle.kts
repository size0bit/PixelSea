plugins {
    id("pixelsea.android.library")
    id("pixelsea.android.compose")
    id("pixelsea.android.hilt")
}

android {
    namespace = "com.pixelsea.feature.gallery"
}
dependencies {
    // 依赖我们刚刚建好的核心 UI 模块，以便使用 MVI 基类等通用 UI 组件
    implementation(project(":core:ui"))

    // 👇 新增：依赖数据层，以便 ViewModel 能拿到 Repository
    implementation(project(":core:data"))

    // 👇 新增：我们在 libs.versions.toml 里定义好的 Coil 依赖
    implementation(libs.coil.compose)

    // ViewModel 的 Compose 支持
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
    // Paging 3 Compose 支持
    implementation(libs.androidx.paging.compose)
}