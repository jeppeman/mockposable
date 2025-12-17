package com.jeppeman.mockposable.compiler

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
@Suppress("unused") // Invoked by kotlinc
@AutoService(CompilerPluginRegistrar::class)
class MockposablePlugin : CompilerPluginRegistrar() {
    override val pluginId: String = MOCKPOSABLE_PLUGIN_ID
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val plugins = configuration.get(KEY_MOCK_PLUGINS, "").split(";")
        val messageCollector = MockposableMessageCollector(
            configuration.get(
                CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                MessageCollector.NONE
            )
        )

        for (extension in plugins) {
            // No extra transformations needed for compose-ui
            if (extension == "compose-ui") continue

            IrGenerationExtension.registerExtension(
                when (extension) {
                    "mockito" -> MockitoIrGenerationExtension(messageCollector)
                    "mockk" -> MockKIrGenerationExtension(messageCollector)
                    else -> throw IllegalArgumentException("Unsupported plugin extension: $extension")
                }
            )
        }
    }
}

private class MockposableMessageCollector(
    private val proxy: MessageCollector
) : MessageCollector by proxy {
    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?
    ) {
        proxy.report(severity, "$LOG_TAG $message", location)
    }
}

private const val LOG_TAG = "[Mockposable]"