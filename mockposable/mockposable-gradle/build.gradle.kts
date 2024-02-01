import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    id("java-gradle-plugin")
}

sourceSets { main { java.srcDir("$buildDir/generated/sources/version/kotlin/main") } }

val copyVersionProvider = tasks.register<Copy>("copyVersion") {
    inputs.property("version", project.property("VERSION_NAME"))
    from(project.layout.projectDirectory.dir("version"))
    into(project.layout.buildDirectory.dir("generated/sources/version/kotlin/main"))
    val projectVersion = project.property("VERSION_NAME")
    val group = project.property("GROUP")
    expand(
        mapOf(
            "projectVersion" to projectVersion,
            "mockposableMockitoCoordinates" to "${group}:mockposable-runtime-mockito:${projectVersion}",
            "mockposableMockKCoordinates" to "${group}:mockposable-runtime-mockk:${projectVersion}",
            "mockposableComposeUiCoordinates" to "${group}:mockposable-runtime-composeui:${projectVersion}",
            "composeCompilerCoordinates" to libs.compose.compiler.get(),
            "composeRuntimeCoordinates" to libs.compose.runtime.get(),
            "mockKCoordinates" to libs.mockk.core.get(),
            "mockKAndroidCoordinates" to libs.mockk.android.get(),
            "mockitoCoordinates" to libs.mockito.kotlin.get(),
            "mockitoAndroidCoordinates" to libs.mockito.android.get(),
        )
    )
    filteringCharset = "UTF-8"
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(copyVersionProvider)
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation(libs.kotlin.gradle)
}

gradlePlugin {
    plugins {
        register("com.jeppeman.mockposable") {
            id = "com.jeppeman.mockposable"
            implementationClass = "com.jeppeman.mockposable.gradle.MockposableSubPlugin"
        }
    }
}

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

tasks.whenTaskAdded {
    // For some reason signing the plugin marker artifact does not happen by default
    if (name == "publishCom.jeppeman.mockposablePluginMarkerMavenPublicationToMavenRepository") {
        dependsOn("signCom.jeppeman.mockposablePluginMarkerMavenPublication")
    }

    if (name == "publishMavenJavaPublicationToMavenRepository") {
        dependsOn("signMavenJavaPublication", "signPluginMavenPublication")
    }

    if (name == "publishPluginMavenPublicationToMavenRepository") {
        dependsOn("signPluginMavenPublication", "signMavenJavaPublication")
    }
}