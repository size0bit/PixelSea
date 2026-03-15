import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType

/**
 * Android Compose 约定插件
 *
 * 功能说明:
 * - 自动配置 Jetpack Compose 相关设置
 * - 统一注入 Compose 核心依赖和版本管理
 * - 支持 Application 和 Library 两种模块类型
 *
 * 使用方式:
 * 在模块的 build.gradle.kts 中应用此插件即可自动获得 Compose 能力
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    /**
     * 插件应用入口函数
     *
     * @param target 目标项目对象，将对此项目应用 Compose 配置
     */
    override fun apply(target: Project) {
        with(target) {
            // 获取版本目录扩展，用于统一管理依赖版本
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            /**
             * 智能判断模块类型
             *
             * 优先获取 Application 扩展，如果没有则获取 Library 扩展
             * 如果两者都不存在，则抛出错误提示需要先应用 Android 插件
             */
            val extension =
                extensions.findByType<ApplicationExtension>()
                    ?: extensions.findByType<LibraryExtension>()
                    ?: error("在使用 Compose 插件前，必须先应用 com.android.application 或 com.android.library 插件")

            /**
             * 通用配置块
             *
             * 强制转换为通用的 CommonExtension 进行底层统一配置
             * 这样无论是 Application 还是 Library 模块都能应用相同的配置
             */
            (extension as CommonExtension<*, *, *, *, *, *>).apply {
                /**
                 * 构建特性配置
                 *
                 * compose = true: 启用 Jetpack Compose 支持
                 */
                buildFeatures {
                    compose = true
                }

                /**
                 * Compose 编译器选项配置
                 *
                 * kotlinCompilerExtensionVersion: 指定 Kotlin 编译器的 Compose 扩展版本
                 * 版本 1.5.15 与 Kotlin 1.9.x 系列兼容，并支持 Compose 1.7.x API
                 */
                @Suppress("UnstableApiUsage")
                composeOptions {
                    kotlinCompilerExtensionVersion = "1.5.15"
                }
            }

            /**
             * 依赖管理配置
             *
             * 统一注入 Compose 核心依赖和 BOM (Bill of Materials)
             * 确保所有使用该插件的模块都使用相同版本的 Compose 库
             */
            dependencies {
                // 获取 Compose BOM，用于统一管理所有 Compose 库的版本
                val bom = libs.findLibrary("androidx-compose-bom").get()
                // 为主代码和测试代码分别添加 BOM 平台依赖
                add("implementation", platform(bom))
                add("androidTestImplementation", platform(bom))
                add("implementation", libs.findLibrary("androidx-animation").get())

                /**
                 * Compose 预览工具依赖
                 *
                 * - androidx-ui-tooling-preview: UI 预览功能库，支持@Preview 注解
                 * - androidx-ui-tooling: UI 调试工具库，仅在 debug 模式下使用
                 */
                add("implementation", libs.findLibrary("androidx-ui-tooling-preview").get())
                add("debugImplementation", libs.findLibrary("androidx-ui-tooling").get())

                /**
                 * Material Design 3 组件库
                 *
                 * 加上这一行，把 Material 3 下发给所有使用该插件的 UI 模块
                 * 提供符合 Material Design 3 规范的 UI 组件和主题样式
                 */
                add("implementation", libs.findLibrary("androidx-material3").get())
            }
        }
    }
}
