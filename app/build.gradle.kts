plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("pixelsea.android.compose")
    id("pixelsea.android.hilt") // 👇 补上这行，让 App 模块拥有依赖注入能力！
}

android {
    namespace = "com.example.pixelsea"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.pixelsea"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

    }
    kotlinOptions {
        jvmTarget = "17"
    }
    /*buildFeatures {
        compose = true
    }
    // 加上这段代码，显式指定匹配 Kotlin 1.9.22 的 Compose 编译器版本
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }*/
}

dependencies {

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    // 引入相册业务模块
    implementation(project(":feature:gallery"))
}