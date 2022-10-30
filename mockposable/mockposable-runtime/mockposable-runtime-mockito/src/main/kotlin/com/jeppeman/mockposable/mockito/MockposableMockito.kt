package com.jeppeman.mockposable.mockito

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import com.jeppeman.mockposable.runComposableOneShot
import org.mockito.ArgumentMatchers
import org.mockito.InOrder
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.*
import org.mockito.stubbing.OngoingStubbing
import org.mockito.stubbing.Stubber
import org.mockito.verification.VerificationMode

/**
 * @see [KStubbing.on]
 */
fun <T, R> KStubbing<T>.onComposable(stubBlock: @Composable T.() -> R): OngoingStubbing<R> = try {
    runComposableOneShot { Mockito.`when`(mock.stubBlock()) }
} catch (e: NullPointerException) {
    throw MockitoKotlinException(
        "NullPointerException thrown when stubbing.\nThis may be due to two reasons:\n\t- The method you're trying to stub threw an NPE: look at the stack trace below;\n\t- You're trying to stub a generic method: try `onGeneric` instead.",
        e
    )
}

/**
 * @see [Stubber.when]
 */
fun <T, R> Stubber.onComposable(mock: T, stubBlock: @Composable T.() -> R) {
    val m = this.whenever(mock)
    runComposableOneShot { m.stubBlock() }
}

/**
 * @see [OngoingStubbing.thenAnswer]
 */
infix fun <T> OngoingStubbing<T>.doAnswerComposable(
    answer: @Composable (InvocationOnMock) -> T?
): OngoingStubbing<T> = thenAnswer { invocation -> runComposableOneShot { answer(invocation) } }

/**
 * @see [Mockito.doAnswer]
 */
fun <T> doAnswerComposable(
    answer: @Composable (InvocationOnMock) -> T?
): Stubber = Mockito.doAnswer { invocation -> runComposableOneShot { answer(invocation) } }

/**
 * @see [InOrder.verify]
 */
fun <T> InOrder.verifyComposable(
    mock: T,
    verifyBlock: @Composable T.() -> Unit
): T = verifyComposable(mock, times(1), verifyBlock)

/**
 * @see [InOrder.verify]
 */
fun <T> InOrder.verifyComposable(
    mock: T,
    mode: VerificationMode,
    verifyBlock: @Composable T.() -> Unit
): T {
    val v = verify(mock, mode)
    runComposableOneShot { v.verifyBlock() }
    return v
}

/**
 * @see [verify]
 */
fun <T> verifyComposable(
    mock: T,
    verifyBlock: @Composable T.() -> Unit
) {
    val m = Mockito.verify(mock)
    runComposableOneShot { m.verifyBlock() }
}

/**
 * @see [verify]
 */
fun <T> verifyComposable(
    mock: T,
    mode: VerificationMode,
    verifyBlock: @Composable T.() -> Unit
) {
    val m = Mockito.verify(mock, mode)
    runComposableOneShot { m.verifyBlock() }
}

/**
 * Like [any] but takes a real object as a parameter and returns it. This is used by the
 * compiler plugin to enable any [Composer] matching while passing the real object so
 * as to not compromise behaviour of concrete mocks.
 */
@Suppress("unused") // Invoked by the compiler plugin
fun <T : Any> any(value: T): T {
    ArgumentMatchers.any(value::class.java)
    return value
}