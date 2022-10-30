import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME

apply(
    from = rootProject.file(
        rootDir.parentFile
            .toPath()
            .resolve("gradle")
            .resolve("gradle-mvn-push.gradle")
    )
)

plugins {
    kotlin("jvm")
}

dependencies {
    api(libs.compose.runtime)
    add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, libs.compose.compiler)
}
