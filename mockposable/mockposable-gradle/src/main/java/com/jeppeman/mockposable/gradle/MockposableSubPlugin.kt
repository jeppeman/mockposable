package com.jeppeman.mockposable.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

@Suppress("unused") // Invoked by gradle
class MockposableSubPlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.extensions.create("mockposable", MockposableSubPluginExtension::class.java)
    }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(MockposableSubPluginExtension::class.java)

        if (extension.plugins.isEmpty()) {
            throw IllegalArgumentException(
                """
                [Mockposable] no mock plugins provided, choose among ${mockPluginDependencyMap.keys}
                Declare as follows:
                
                .gradle 
                mockposable {
                    plugins = [...]
                }
                
                .gradle.kts
                mockposable {
                    plugins = listOf(...)
                }
            """.trimIndent()
            )
        }

        extension.plugins.forEach { mockPlugin ->
            val mockPluginDependencies = mockPluginDependencyMap[mockPlugin]
                ?: throw IllegalArgumentException(
                    "[Mockposable] unsupported mock plugin $mockPlugin, choose among ${mockPluginDependencyMap.keys}."
                )

            mockPluginDependencies["jvm"]?.forEach { jvmDepCoordinates ->
                project.configurations.findByName("testImplementation")
                    ?.dependencies
                    ?.apply { add(project.dependencies.create(jvmDepCoordinates)) }
            }

            mockPluginDependencies["android"]?.forEach { androidDepCoordinates ->
                project.configurations.findByName("androidTestImplementation")
                    ?.dependencies
                    ?.apply { add(project.dependencies.create(androidDepCoordinates)) }
            }
        }

        project.configurations
            .getByName("implementation")
            .dependencies
            .add(project.dependencies.create(COMPOSE_RUNTIME_COORDINATES))

        val composeCompilerCoordinates = if (extension.composeCompilerPluginVersion.isNotBlank()) {
            COMPOSE_COMPILER_COORDINATES.dropLastWhile { it != ':' } + extension.composeCompilerPluginVersion
        } else {
            COMPOSE_COMPILER_COORDINATES
        }

        project.configurations
            .getByName(PLUGIN_CLASSPATH_CONFIGURATION_NAME)
            .dependencies
            .add(project.dependencies.create(composeCompilerCoordinates))

        return project.provider {
            listOf(
                SubpluginOption(
                    key = "plugins",
                    value = extension.plugins.joinToString(";")
                )
            )
        }
    }

    override fun getCompilerPluginId(): String = "com.jeppeman.mockposable"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.jeppeman.mockposable",
        artifactId = "mockposable-compiler",
        version = PROJECT_VERSION
    )

    override fun isApplicable(
        kotlinCompilation: KotlinCompilation<*>
    ): Boolean = true
}

private val mockPluginDependencyMap: Map<String, Map<String, List<String>>> = mapOf(
    MOCKK to mapOf(
        "jvm" to MOCKK_JVM_DEPENDENCIES,
        "android" to MOCKK_ANDROID_DEPENDENCIES,
    ),
    MOCKITO to mapOf(
        "jvm" to MOCKITO_JVM_DEPENDENCIES,
        "android" to MOCKITO_ANDROID_DEPENDENCIES,
    ),
    COMPOSE_UI to mapOf(
        "jvm" to COMPOSE_UI_JVM_DEPENDENCIES,
        "android" to COMPOSE_UI_ANDROID_DEPENDENCIES,
    )
)