dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // 关键配置：让 build-logic 也能读取到主工程的 libs.versions.toml 版本目录
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
// 包含一个名为 convention 的子模块，稍后我们将把自定义插件写在这里
include(":convention")