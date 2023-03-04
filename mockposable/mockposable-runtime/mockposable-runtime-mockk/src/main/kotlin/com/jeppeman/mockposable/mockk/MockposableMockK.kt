package com.jeppeman.mockposable.mockk

import androidx.compose.runtime.Composable
import com.jeppeman.mockposable.runComposableOneShot
import io.mockk.*

/**
 * @see [every]
 */
fun <T> everyComposable(
    stubBlock: @Composable MockKMatcherScope.() -> T
): MockKStubScope<T, T> = MockK.useImpl {
    MockKDsl.internalEvery { runComposableOneShot { stubBlock() } }
}

/**
 * @see [MockKStubScope.answers]
 */
infix fun <T, B> MockKStubScope<T, B>.answersComposable(
    answer: @Composable MockKAnswerScope<T, B>.(Call) -> T
): MockKAdditionalAnswerScope<T, B> = answers { call ->
    runComposableOneShot(true) { answer(call) }
}

/**
 * @see [MockKAdditionalAnswerScope.andThenAnswer]
 */
infix fun <T, B> MockKAdditionalAnswerScope<T, B>.andThenComposable(
    answer: @Composable MockKAnswerScope<T, B>.(Call) -> T
): MockKAdditionalAnswerScope<T, B> = andThenAnswer { call ->
    runComposableOneShot(true) { answer(call) }
}

/**
 * @see [verify]
 */
fun verifyComposable(
    ordering: Ordering = Ordering.UNORDERED,
    inverse: Boolean = false,
    atLeast: Int = 1,
    atMost: Int = Int.MAX_VALUE,
    exactly: Int = -1,
    timeout: Long = 0,
    verifyBlock: @Composable MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalVerify(
        ordering = ordering,
        inverse = inverse,
        atLeast = atLeast,
        atMost = atMost,
        exactly = exactly,
        timeout = timeout,
        verifyBlock = { runComposableOneShot { verifyBlock() } }
    )
}

/**
 * @see [verifyAll]
 */
fun verifyComposableAll(
    inverse: Boolean = false,
    verifyBlock: @Composable MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalVerifyAll(
        inverse = inverse,
        verifyBlock = { runComposableOneShot { verifyBlock() } }
    )
}

/**
 * @see [verifyOrder]
 */
fun verifyComposableOrder(
    inverse: Boolean = false,
    verifyBlock: @Composable MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalVerifyOrder(
        inverse = inverse,
        verifyBlock = { runComposableOneShot { verifyBlock() } }
    )
}

/**
 * @see [verifySequence]
 */
fun verifyComposableSequence(
    inverse: Boolean = false,
    verifyBlock: @Composable MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalVerifySequence(
        inverse = inverse,
        verifyBlock = { runComposableOneShot { verifyBlock() } }
    )
}
