package com.jeppeman.mockposable.composeui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.jeppeman.mockposable.Mockposable
import org.junit.runner.Description
import org.junit.runners.model.Statement

class MockposableComposeRule(
    private val real: ComposeContentTestRule
) : ComposeContentTestRule by real {
    private var content: (@Composable () -> Unit)? = null

    private val composerContent: @Composable () -> Unit = {
        Mockposable.activeComposer = currentComposer
        content?.invoke()
    }

    override fun setContent(composable: @Composable () -> Unit) {
        content = composable
        real.setContent(composerContent)
    }

    override fun apply(base: Statement, description: Description): Statement {
        val realStatement = real.apply(base, description)
        return object : Statement() {
            override fun evaluate() {
                try {
                    realStatement.evaluate()
                } finally {
                    Mockposable.activeComposer = null
                }
            }
        }
    }
}
