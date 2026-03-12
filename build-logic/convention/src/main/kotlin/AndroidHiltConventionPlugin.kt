import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                // 自动应用 Hilt 和 KSP 插件
                apply("com.google.devtools.ksp")
                apply("com.google.dagger.hilt.android")
            }

            // 获取我们在 libs.versions.toml 中定义的依赖
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            dependencies {
                // 自动为应用的模块添加依赖，无需在每个模块里再写一遍
                "implementation"(libs.findLibrary("hilt-android").get())
                "ksp"(libs.findLibrary("hilt-compiler").get())
            }
        }
    }
}