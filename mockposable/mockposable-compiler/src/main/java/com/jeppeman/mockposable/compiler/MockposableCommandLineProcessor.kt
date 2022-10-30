package com.jeppeman.mockposable.compiler

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

private const val ARG_NAME_MOCK_PLUGINS = "plugins"

val KEY_MOCK_PLUGINS = CompilerConfigurationKey<String>(ARG_NAME_MOCK_PLUGINS)

@Suppress("unused") // Invoked by the compiler
@AutoService(CommandLineProcessor::class)
class MockposableCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "com.jeppeman.mockposable"

    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(ARG_NAME_MOCK_PLUGINS, "String", "", required = true)
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) = when (option.optionName) {
        ARG_NAME_MOCK_PLUGINS -> configuration.put(KEY_MOCK_PLUGINS, value)
        else -> error("Unknown plugin option: ${option.optionName}")
    }
}