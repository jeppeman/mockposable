import com.jeppeman.mockposable.gradle.MOCKITO
import com.jeppeman.mockposable.gradle.MOCKK
import com.jeppeman.mockposable.gradle.COMPOSE_UI

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.jeppeman.mockposable")
    id("app.cash.molecule")
    id("app.cash.paparazzi")
}

mockposable {
    plugins = listOf(MOCKK, MOCKITO, COMPOSE_UI)
    composeCompilerPluginVersion = libs.versions.compose.compiler.get()
}

android {
    compileSdk = 34
    namespace = "com.jeppeman.mockposable.integrationtests.android"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdk = 21
        val taskNames = project.gradle
            .startParameter
            .taskNames
            .joinToString(",") { "\"${it}\"" }
        buildConfigField(
            type = "String[]",
            name = "GRADLE_TASKS",
            value = "new String[] { $taskNames }"
        )
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
        animationsDisabled = true
    }

    packagingOptions {
        resources.excludes.add("META-INF/LICENSE*")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.fragment.testing)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.test.manifest)

    testImplementation(libs.hamcrest)
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.espresso.core)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core)
}