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
    id("com.google.devtools.ksp")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    compileOnly(libs.kotlin.compiler)
    implementation(libs.autoservice.annotations)
    ksp(libs.autoservice.ksp)

    testImplementation(libs.junit)
    testImplementation(libs.compile.testing)
    testImplementation(libs.compose.runtime)
    testImplementation(libs.compose.compiler)
    testImplementation(libs.kotlin.compiler)
    testImplementation(libs.mockk.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(project(":mockposable-runtime:mockposable-runtime-mockk"))
    testImplementation(project(":mockposable-runtime:mockposable-runtime-mockito"))
}