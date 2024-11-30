buildscript {
    apply(
        from = rootProject.file(
            rootDir.parentFile
                .toPath()
                .resolve("gradle")
                .resolve("gradle-mvn-push.gradle")
        )
    )
}

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
}

android {
    compileSdk = 34
    namespace = "com.jeppeman.mockposable.composeui"

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    api(libs.compose.ui)
    api(libs.compose.ui.test.junit4)
    api(project(":mockposable-runtime"))
}
