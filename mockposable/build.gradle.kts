import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
        mavenLocal()
    }

    dependencies {
        classpath(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
        classpath(libs.android.gradle)
        classpath(libs.google.ksp)
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

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "17"
        }
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

    tasks.whenTaskAdded {
        if (name == "generateMetadataFileForAarPublication") {
            dependsOn("androidSourcesJar")
        }
    }
}

task("clean", Delete::class) {
    delete(project.buildDir)
}

