plugins {
    `kotlin-dsl`
}

group = "com.pixelsea.buildlogic"

// 统一使用 Java 17
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // 将这里的 8.3.0 改为 8.9.1
    implementation("com.android.tools.build:gradle:8.9.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
}
gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "pixelsea.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        // 新增 Hilt 插件注册
        register("androidHilt") {
            id = "pixelsea.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        // 新增 Compose 插件注册
        register("androidCompose") {
            id = "pixelsea.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
    }
}