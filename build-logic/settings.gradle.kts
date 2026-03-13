/**
 * Build-Logic 项目的设置配置文件
 * 
 * 功能说明:
 * - 配置依赖解析策略和仓库源
 * - 管理版本目录，实现统一的依赖版本控制
 * - 定义项目结构和子模块
 */
dependencyResolutionManagement {
    /**
     * 依赖仓库配置
     * 
     * 指定 Gradle 从以下仓库获取依赖:
     * - google(): Google Maven 仓库，提供 Android 相关库
     * - mavenCentral(): Maven 中央仓库，提供大多数开源库
     * - gradlePluginPortal(): Gradle 插件门户，提供 Gradle 插件
     */
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    /**
     * 版本目录配置
     * 
     * 关键配置：让 build-logic 也能读取到主工程的 libs.versions.toml 版本目录
     * 这样可以实现整个项目的依赖版本统一管理
     * 
     * 配置说明:
     * - create("libs"): 创建名为 "libs" 的版本目录
     * - from(files("../gradle/libs.versions.toml")): 指定版本目录文件路径为主工程的 TOML 配置文件
     */
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

/**
 * 项目根节点配置
 * 
 * rootProject.name: 设置项目根目录名称为 "build-logic"
 */
rootProject.name = "build-logic"
/**
 * 子模块包含配置
 * 
 * include(":convention"): 包含一个名为 convention 的子模块
 * 用途：将在此子模块中编写自定义 Gradle 插件逻辑
 */
include(":convention")