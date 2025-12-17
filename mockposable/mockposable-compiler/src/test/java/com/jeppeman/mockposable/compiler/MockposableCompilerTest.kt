package com.jeppeman.mockposable.compiler

import androidx.compose.compiler.plugins.kotlin.ComposeCommandLineProcessor
import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class MockposableCompilerTest {
    @Test
    fun `GIVEN mockk plugin applied WHEN compiling THEN should compile successfully`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "main.kt", """
import androidx.compose.runtime.Composable
import com.jeppeman.mockposable.mockk.everyComposable
import com.jeppeman.mockposable.mockk.verifyComposable

@Composable
fun dummyComposable(arg: Int): Int {
    return arg + 18
}

fun main() {
    everyComposable {
       dummyComposable(any())
    } returns 7

    verifyComposable { dummyComposable(any()) }
}
"""
            ),
            "mockk"
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertTrue(result.messages.contains("[Mockposable] Running MockK composable transformations"))
        assertTrue(result.messages.contains("[Mockposable] Transformed dummyComposable(arg = \$this\$everyComposable"))
        assertTrue(result.messages.contains("[Mockposable] Transformed dummyComposable(arg = \$this\$verifyComposable"))
    }

    @Test
    fun `GIVEN mockito plugin applied WHEN compiling THEN should compile successfully`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "main.kt", """
import androidx.compose.runtime.Composable
import org.mockito.kotlin.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import com.jeppeman.mockposable.mockito.onComposable
import com.jeppeman.mockposable.mockito.verifyComposable

interface Dummy {
    @Composable
    fun dummyComposable(arg: Int): Int {
        return arg + 18
    }
}

fun main() {
    val dummyMock = mock<Dummy> {
        onComposable { dummyComposable(any()) } doReturn 7
    }

    verifyComposable(dummyMock) { dummyComposable(any()) }
}
"""
            ),
             "mockito"
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertTrue(result.messages.contains("[Mockposable] Running Mockito composable transformations"))
        assertTrue(result.messages.contains("[Mockposable] Transformed \$this\$onComposable.dummyComposable(arg = any<Int>()"))
        assertTrue(result.messages.contains("[Mockposable] Transformed \$this\$verifyComposable.dummyComposable(arg = any<Int>()"))
    }
}

@OptIn(ExperimentalCompilerApi::class)
fun compile(
    sourceFiles: List<SourceFile>,
    vararg plugins: String,
): CompilationResult {
    val mockposableCommandLineProcessor = MockposableCommandLineProcessor()
    val composeCommandLineProcessor = ComposeCommandLineProcessor()
    return KotlinCompilation().apply {
        sources = sourceFiles
        jvmTarget = JvmTarget.JVM_21.description
        verbose = true
        commandLineProcessors = listOf(mockposableCommandLineProcessor, composeCommandLineProcessor)
        compilerPluginRegistrars = listOf(ComposePluginRegistrar(), MockposablePlugin())
        pluginOptions = listOf(
            PluginOption(
                pluginId = mockposableCommandLineProcessor.pluginId,
                optionName = "plugins",
                optionValue = plugins.joinToString(";")
            ),
        )
        inheritClassPath = true
    }.compile()
}

@OptIn(ExperimentalCompilerApi::class)
fun compile(
    sourceFile: SourceFile,
    vararg plugins: String
): CompilationResult {
    return compile(listOf(sourceFile), *plugins)
}
