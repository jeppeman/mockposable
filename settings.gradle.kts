rootProject.name = "mockposable-root"

include("integration-tests:android")
include("integration-tests:jvm")

includeBuild("mockposable") {
    dependencySubstitution {
        substitute(module("com.jeppeman.mockposable:mockposable-compiler"))
            .using(project(":mockposable-compiler"))

        substitute(module("com.jeppeman.mockposable:mockposable-gradle"))
            .using(project(":mockposable-gradle"))

        substitute(module("com.jeppeman.mockposable:mockposable-runtime"))
            .using(project(":mockposable-runtime"))

        substitute(module("com.jeppeman.mockposable:mockposable-runtime-mockk"))
            .using(project(":mockposable-runtime:mockposable-runtime-mockk"))

        substitute(module("com.jeppeman.mockposable:mockposable-runtime-mockito"))
            .using(project(":mockposable-runtime:mockposable-runtime-mockito"))
    }
}