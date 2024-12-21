import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }

    dependencies {
        classpath(libs.android.gradle)
        classpath(libs.paparazzi)
        classpath("com.jeppeman.mockposable:mockposable-gradle")
    }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version libs.versions.kotlin.get() apply false
    alias(libs.plugins.compose.compiler) apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }

    tasks.withType(Test::class.java).configureEach {
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            showCauses = true
            showExceptions = true
            showStackTraces = true
            showStandardStreams = true
            events = setOf(
                TestLogEvent.PASSED,
                TestLogEvent.SKIPPED,
                TestLogEvent.FAILED,
                TestLogEvent.STANDARD_OUT,
                TestLogEvent.STANDARD_ERROR
            )
        }
    }

    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
        kotlinOptions {
            jvmTarget = JvmTarget.JVM_11.target
        }
    }

    tasks.withType(JavaCompile::class).configureEach {
        sourceCompatibility = JavaVersion.VERSION_11.toString()
        targetCompatibility = JavaVersion.VERSION_11.toString()
    }

    plugins.withType(com.android.build.gradle.BasePlugin::class).configureEach {
        val android = extensions.getByName("android") as com.android.build.gradle.BaseExtension
        android.compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }
}

task("clean", Delete::class) {
    delete(rootProject.buildDir)
}