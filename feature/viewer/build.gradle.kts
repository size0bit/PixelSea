plugins {
    id("pixelsea.android.library")
    id("pixelsea.android.compose")
    id("pixelsea.android.hilt")
}

android {
    namespace = "com.pixelsea.feature.viewer"
}
dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:data")) // 👇 依赖数据层以获取 Repository
    // 预览页面同样需要加载图片
    implementation(libs.coil.compose)
    implementation(libs.androidx.paging.compose) // 👇 Paging 3 Compose 支持

    // ViewModel 和 Hilt 的 Compose 支持
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
}
