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
}

android {
    compileSdk = 34
    namespace = "com.jeppeman.mockposable.composeui"

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(libs.compose.ui)
    api(libs.compose.ui.test.junit4)
    api(project(":mockposable-runtime"))
}
