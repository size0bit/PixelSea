plugins {
    // 这就是我们刚才自己写的插件 ID！
    id("pixelsea.android.library")
    id("pixelsea.android.hilt") // 这一行就搞定了 Hilt 和 KSP 的所有配置！
}

android {
    // AGP 8.0 之后，包名必须在这里声明
    namespace = "com.pixelsea.core.data"
}