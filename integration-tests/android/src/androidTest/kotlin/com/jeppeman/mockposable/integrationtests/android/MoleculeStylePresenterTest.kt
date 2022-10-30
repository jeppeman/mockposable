package com.jeppeman.mockposable.integrationtests.android

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jeppeman.mockposable.mockk.everyComposable
import io.mockk.MockKMatcherScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.flow.MutableSharedFlow
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun MockKMatcherScope.matchFirst(
    predicate: (TestEvent?) -> Boolean
) = match<MutableSharedFlow<TestEvent>> { flow -> predicate(flow.replayCache.firstOrNull()) }

@RunWith(AndroidJUnit4::class)
class ComposableMoleculePresenterTest {
    private lateinit var recomposeScope: RecomposeScope
    private val events = object : ProxyMutableSharedFlow<TestEvent>() {
        override fun tryEmit(value: TestEvent): Boolean = super.tryEmit(value).apply {
            recomposeScope.invalidate()
        }
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun topLevelComposableWithMoleculeStylePresenter() {
        val fakeErrorModel = TestModel.Error("Failed")
        val fakeDataModel = TestModel.Data(2)
        val mockPresenter = mockk<TestPresenter> {
            everyComposable { present(matchFirst { it == null }) } returnsMany listOf(
                TestModel.Loading,
                fakeErrorModel
            )
            everyComposable { present(matchFirst { it == TestEvent.Reload }) } returns fakeDataModel
        }

        composeTestRule.setContent {
            recomposeScope = currentRecomposeScope
            ComposeTestView(mockPresenter, events)
        }

        // First progress is displayed
        composeTestRule.onNodeWithTag("progress").assertIsDisplayed()
        recomposeScope.invalidate() // Simulates loading finished by just recomposing
        // Then error is displayed
        composeTestRule.onNodeWithText(fakeErrorModel.message).assertIsDisplayed()
        // Press "Try again" and reload
        composeTestRule.onNodeWithText("Try again").performClick()
        // Then the actual data is displayed
        composeTestRule.onNodeWithText(fakeDataModel.data.toString()).assertIsDisplayed()
    }
}

@RunWith(AndroidJUnit4::class)
class FragmentMoleculePresenterTest {
    private lateinit var recomposeScope: RecomposeScope
    private val mockPresenter: TestPresenter = mockk()

    private fun launch(): FragmentScenario<TestFragment> = launchFragmentInContainer(
        factory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return TestFragment().apply {
                    present = { events ->
                        val proxyEvents = object : ProxyMutableSharedFlow<TestEvent>(events) {
                            override fun tryEmit(
                                value: TestEvent
                            ): Boolean = super.tryEmit(value).apply {
                                recomposeScope.invalidate()
                            }
                        }
                        recomposeScope = currentRecomposeScope
                        mockPresenter.present(events = proxyEvents)
                    }
                }
            }
        }
    )

    @Before
    fun setUp() {
        // CI workaround for https://github.com/ReactiveCircus/android-emulator-runner/issues/129
        ApplicationProvider.getApplicationContext<Application>()
            .sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    @Test
    fun fragmentWithMoleculeStylePresenter() {
        val fakeError = TestModel.Error("Failed")
        val fakeData = TestModel.Data(2)
        everyComposable { mockPresenter.present(matchFirst { it == null }) } returnsMany listOf(
            TestModel.Loading,
            fakeError
        )
        everyComposable { mockPresenter.present(matchFirst { it == TestEvent.Reload }) } returns fakeData

        launch()

        // First progress is displayed
        onView(withTagValue(equalTo("progress"))).check(matches(isDisplayed()))
        recomposeScope.invalidate() // Simulates loading finished by just recomposing
        // Then error is displayed
        onView(withText(equalTo(fakeError.message))).check(matches(isDisplayed()))
        // Press "Try again" and reload
        onView(withText(equalTo("Try again"))).perform(click())
        // Then the actual data is displayed
        onView(withText(fakeData.data.toString()))
    }
}
