import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Android Hilt 依赖注入约定插件
 *
 * 功能说明:
 * - 自动应用 Hilt 和 KSP (Kotlin Symbol Processing) 插件
 * - 统一注入 Hilt 相关依赖，无需在每个模块中重复配置
 * - 简化依赖注入的搭建流程
 *
 * 使用方式:
 * 在模块的 build.gradle.kts 中应用此插件即可自动获得 Hilt 依赖注入能力
 */
class AndroidHiltConventionPlugin : Plugin<Project> {
    /**
     * 插件应用入口函数
     *
     * @param target 目标项目对象，将对此项目应用 Hilt 配置
     */
    override fun apply(target: Project) {
        with(target) {
            /**
             * 插件管理器配置
             *
             * 自动应用以下两个关键插件:
             * - com.google.devtools.ksp: Kotlin 符号处理插件，用于代码生成
             * - com.google.dagger.hilt.android: Hilt 依赖注入框架插件
             */
            with(pluginManager) {
                apply("com.google.devtools.ksp")
                apply("com.google.dagger.hilt.android")
            }

            // 获取版本目录扩展，用于从 libs.versions.toml 读取依赖定义
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            /**
             * 依赖管理配置
             *
             * 自动为应用模块添加 Hilt 核心依赖:
             * - hilt-android: Hilt 运行时库，提供依赖注入功能
             * - hilt-compiler: Hilt 编译器，通过 KSP 生成所需的注入代码
             *
             * 优势：所有使用该插件的模块都无需重复编写这些依赖配置
             */
            dependencies {
                "implementation"(libs.findLibrary("hilt-android").get())
                "ksp"(libs.findLibrary("hilt-compiler").get())
            }
        }
    }
}
