package com.jeppeman.mockposable.integrationtests.android

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.androidHome
import app.cash.paparazzi.detectEnvironment
import com.jeppeman.mockposable.mockk.everyComposable
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class SnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "Theme.Material",
        environment = detectEnvironment().copy(
            platformDir = "${androidHome()}/platforms/android-34",
            compileSdkVersion = 34
        )
    )

    private val isRecord = BuildConfig.GRADLE_TASKS.any { it.contains("recordPaparazzi") }
    private val isVerify = BuildConfig.GRADLE_TASKS.any { it.contains("verifyPaparazzi") }

    @Test
    fun `record and verify snapshots with mocked pressed state should succeed`() {
        when {
            isRecord -> paparazzi.snapshot { RecordingView() }
            isVerify -> paparazzi.snapshot {
                mockkStatic(PRESS_INTERACTION)
                everyComposable {
                    fakeInteractionSource.collectIsPressedAsState()
                } returns mutableStateOf(true)

                VerifyingView(interactionSource = fakeInteractionSource)

                unmockkStatic(PRESS_INTERACTION)
            }
        }
    }

//    This fails
//    @Test
//    fun `record and verify snapshots with without mocked pressed state should fail`() {
//        when {
//            isRecord -> paparazzi.snapshot { RecordingView() }
//            isVerify -> {
//                paparazzi.snapshot { VerifyingView(interactionSource = fakeInteractionSource) }
//            }
//        }
//    }
}

private val fakeInteractionSource = object : MutableInteractionSource {
    override val interactions: Flow<Interaction> = flowOf(PressInteraction.Press(Offset.Zero))
    override suspend fun emit(interaction: Interaction) = Unit
    override fun tryEmit(interaction: Interaction): Boolean = false
}

@Composable
fun RecordingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red)
    ) {
        Button(onClick = { }) {
            Text(text = "Press me")
        }
    }
}

@Composable
fun VerifyingView(interactionSource: MutableInteractionSource) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .composed {
                val isPressed by interactionSource.collectIsPressedAsState()
                background(if (isPressed) Color.Red else Color.Blue)
            }

    ) {
        Button(onClick = { }, interactionSource = interactionSource) {
            Text(text = "Press me")
        }
    }
}