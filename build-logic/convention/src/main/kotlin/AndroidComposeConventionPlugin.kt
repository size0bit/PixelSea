import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.findByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            // 智能判断：优先获取 Application 扩展，如果没有则获取 Library 扩展
            val extension = extensions.findByType<ApplicationExtension>()
                ?: extensions.findByType<LibraryExtension>()
                ?: error("在使用 Compose 插件前，必须先应用 com.android.application 或 com.android.library 插件")

            // 强制转换为通用的 CommonExtension 进行底层统一配置
            (extension as CommonExtension<*, *, *, *, *, *>).apply {
                buildFeatures {
                    compose = true
                }

                @Suppress("UnstableApiUsage")
                composeOptions {
                    kotlinCompilerExtensionVersion = "1.5.10"
                }
            }

            // 统一注入 Compose 核心依赖和 BOM
            dependencies {
                val bom = libs.findLibrary("androidx-compose-bom").get()
                add("implementation", platform(bom))
                add("androidTestImplementation", platform(bom))

                // 预览工具和基础 UI 库
                add("implementation", libs.findLibrary("androidx-ui-tooling-preview").get())
                add("debugImplementation", libs.findLibrary("androidx-ui-tooling").get())

                // 👇 加上这一行，把 Material 3 下发给所有使用该插件的 UI 模块！
                add("implementation", libs.findLibrary("androidx-material3").get())
            }
        }
    }
}