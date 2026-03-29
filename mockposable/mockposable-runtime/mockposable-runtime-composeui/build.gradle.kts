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
    alias(libs.plugins.compose.compiler)
}

android {
    compileSdk = 36
    namespace = "com.jeppeman.mockposable.composeui"

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    api(libs.compose.ui)
    api(libs.compose.ui.test.junit4)
    api(project(":mockposable-runtime"))
}
