import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }

    dependencies {
        classpath(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
        classpath(libs.android.gradle)
        classpath(libs.molecule)
        classpath(libs.paparazzi)
        classpath("com.jeppeman.mockposable:mockposable-gradle")
    }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version libs.versions.kotlin.get() apply false
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
}

task("clean", Delete::class) {
    delete(rootProject.buildDir)
}