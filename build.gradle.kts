// 这里的 alias 会读取我们之前在 libs.versions.toml 中定义的插件版本
// apply false 表示只注册版本号，先不实际应用到根项目
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}