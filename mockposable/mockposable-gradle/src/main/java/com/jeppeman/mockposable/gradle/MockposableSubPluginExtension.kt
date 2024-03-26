package com.jeppeman.mockposable.gradle

abstract class MockposableSubPluginExtension {
    var composeCompilerPluginVersion: String = ""
    var plugins: List<String> = emptyList()
}