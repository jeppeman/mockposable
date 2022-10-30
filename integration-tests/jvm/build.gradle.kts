import com.jeppeman.mockposable.gradle.MOCKITO
import com.jeppeman.mockposable.gradle.MOCKK

plugins {
    kotlin("jvm")
    id("com.jeppeman.mockposable")
}

mockposable {
    plugins = listOf(MOCKK, MOCKITO)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.mockito.inline)
}
