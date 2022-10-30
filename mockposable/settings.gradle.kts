rootProject.name = "mockposable"

include(":mockposable-compiler")
include(":mockposable-gradle")
include(":mockposable-runtime")
include(":mockposable-runtime:mockposable-runtime-mockk")
include(":mockposable-runtime:mockposable-runtime-mockito")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}