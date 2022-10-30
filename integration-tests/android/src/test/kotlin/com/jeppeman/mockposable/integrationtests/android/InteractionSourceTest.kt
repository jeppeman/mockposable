package com.jeppeman.mockposable.integrationtests.android

import androidx.compose.foundation.interaction.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jeppeman.mockposable.mockk.everyComposable
import com.jeppeman.mockposable.runComposableOneShot
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InteractionSourceKtTest {
    @Test
    fun `mocked collectIsPressedAsState`() {
        mockkStatic(PRESS_INTERACTION)
        val interactionSource = MutableInteractionSource()
        everyComposable { interactionSource.collectIsPressedAsState() } returns mutableStateOf(true) andThenAnswer { callOriginal() }

        val result1 = runComposableOneShot { isPressed(interactionSource) }
        val result2 = runComposableOneShot { isPressed(interactionSource) }

        assertTrue(result1)
        assertFalse(result2)
        unmockkStatic(PRESS_INTERACTION)
    }

    @Test
    fun `mocked collectIsDraggedAsState`() {
        mockkStatic(DRAG_INTERACTION)
        val interactionSource = MutableInteractionSource()
        everyComposable { interactionSource.collectIsDraggedAsState() } returns mutableStateOf(true)

        val result = runComposableOneShot { isDragged(interactionSource) }

        assertTrue(result)
        unmockkStatic(DRAG_INTERACTION)
    }

    @Test
    fun `mocked collectIsFocusedAsState`() {
        mockkStatic(FOCUS_INTERACTION)
        val interactionSource = MutableInteractionSource()
        everyComposable { interactionSource.collectIsFocusedAsState() } returns mutableStateOf(true)

        val result = runComposableOneShot { isFocused(interactionSource) }

        assertTrue(result)
        unmockkStatic(FOCUS_INTERACTION)
    }

    @Test
    fun `mocked collectIsHoveredAsState`() {
        mockkStatic(HOVER_INTERACTION)
        val interactionSource = MutableInteractionSource()
        everyComposable { interactionSource.collectIsHoveredAsState() } returns mutableStateOf(true)

        val result = runComposableOneShot { isHovered(interactionSource) }

        assertTrue(result)
        unmockkStatic(HOVER_INTERACTION)
    }
}

@Composable
fun isPressed(interactionSource: InteractionSource): Boolean {
    val isPressed by interactionSource.collectIsPressedAsState()
    return isPressed
}

@Composable
fun isDragged(interactionSource: InteractionSource): Boolean {
    val isDragged by interactionSource.collectIsDraggedAsState()
    return isDragged
}

@Composable
fun isFocused(interactionSource: InteractionSource): Boolean {
    val isFocused by interactionSource.collectIsFocusedAsState()
    return isFocused
}

@Composable
fun isHovered(interactionSource: InteractionSource): Boolean {
    val isHovered by interactionSource.collectIsHoveredAsState()
    return isHovered
}

const val INTERACTION_PACKAGE_NAME = "androidx.compose.foundation.interaction"
const val PRESS_INTERACTION = "${INTERACTION_PACKAGE_NAME}.PressInteractionKt"
const val DRAG_INTERACTION = "${INTERACTION_PACKAGE_NAME}.DragInteractionKt"
const val FOCUS_INTERACTION = "${INTERACTION_PACKAGE_NAME}.FocusInteractionKt"
const val HOVER_INTERACTION = "${INTERACTION_PACKAGE_NAME}.HoverInteractionKt"
