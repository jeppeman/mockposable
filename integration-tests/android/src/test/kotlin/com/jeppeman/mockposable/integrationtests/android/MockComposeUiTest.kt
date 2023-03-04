package com.jeppeman.mockposable.integrationtests.android

import androidx.compose.material.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jeppeman.mockposable.composeui.MockposableComposeRule
import com.jeppeman.mockposable.mockk.answersComposable
import com.jeppeman.mockposable.mockk.everyComposable
import com.jeppeman.mockposable.mockk.verifyComposable
import io.mockk.mockkStatic
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(instrumentedPackages = ["androidx.loader.content"])
@RunWith(AndroidJUnit4::class)
class MockComposeUiTest {
    @get:Rule
    val composeTestRule = MockposableComposeRule(createComposeRule())

    @Test
    fun test() = mockkStatic("com.jeppeman.mockposable.integrationtests.android.TestViewKt") {
        everyComposable { ComposeDummy(name = any()) } answersComposable {
            Text(text = "Will replace")
        }

        composeTestRule.setContent { ComposeDummy(name = "Will be replaced") }

        verifyComposable { ComposeDummy(name = any()) }
        composeTestRule.onNodeWithText("Will replace").assertIsDisplayed()
        composeTestRule.onNodeWithText("Will be replaced").assertDoesNotExist()
    }
}