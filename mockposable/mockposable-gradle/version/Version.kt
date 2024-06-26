@file:JvmName("Version")

package com.jeppeman.mockposable.gradle

internal const val PROJECT_VERSION = "$projectVersion"
internal const val COMPOSE_RUNTIME_COORDINATES = "$composeRuntimeCoordinates"
internal val MOCKITO_JVM_DEPENDENCIES = listOf("$mockitoCoordinates", "$mockposableMockitoCoordinates")
internal val MOCKITO_ANDROID_DEPENDENCIES = listOf("$mockitoCoordinates", "$mockitoAndroidCoordinates", "$mockposableMockitoCoordinates")
internal val MOCKK_JVM_DEPENDENCIES = listOf("$mockKCoordinates", "$mockposableMockKCoordinates")
internal val MOCKK_ANDROID_DEPENDENCIES = listOf("$mockKAndroidCoordinates", "$mockposableMockKCoordinates")
internal val COMPOSE_UI_JVM_DEPENDENCIES = listOf("$mockposableComposeUiCoordinates")
internal val COMPOSE_UI_ANDROID_DEPENDENCIES = listOf("$mockposableComposeUiCoordinates")
