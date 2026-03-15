pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PixelSea"

include(":app")

// 核心支撑模块 (Core)
include(":core:data")
include(":core:network")
include(":core:sync")
include(":core:ui")
include(":core:utils")

// 业务特性模块 (Feature)
include(":feature:gallery")
include(":feature:settings")
include(":feature:viewer")

// 将 build-logic 作为复合构建 (Composite Build) 引入
includeBuild("build-logic")