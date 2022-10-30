import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME

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

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
}

dependencies {
    compileOnly(libs.mockito.kotlin) // implementation is added by the gradle plugin
    api(project(":mockposable-runtime"))

    add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, libs.compose.compiler)
}