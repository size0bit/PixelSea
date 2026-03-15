import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    base
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<KtlintExtension> {
        android.set(true)
        outputToConsole.set(true)
        ignoreFailures.set(false)
        filter {
            exclude("**/build/**")
            exclude("**/generated/**")
            include("**/*.kt")
            include("**/*.kts")
        }
    }
}

tasks.register("lintKotlin") {
    group = "verification"
    description = "Runs ktlint checks for the main build and build-logic."
    dependsOn(subprojects.map { "${it.path}:ktlintCheck" })
    dependsOn(gradle.includedBuild("build-logic").task(":convention:ktlintCheck"))
}

tasks.register("formatKotlin") {
    group = "formatting"
    description = "Formats Kotlin sources for the main build and build-logic."
    dependsOn(subprojects.map { "${it.path}:ktlintFormat" })
    dependsOn(gradle.includedBuild("build-logic").task(":convention:ktlintFormat"))
}

tasks.named("check") {
    dependsOn("lintKotlin")
}
