package com.jeppeman.mockposable.integrationtests.jvm

import androidx.compose.runtime.Composable
import com.jeppeman.mockposable.mockito.doAnswerComposable
import com.jeppeman.mockposable.mockito.onComposable
import com.jeppeman.mockposable.mockito.verifyComposable
import com.jeppeman.mockposable.runComposableOneShot
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.*

class JvmMockitoTest {
    @Test
    fun `composable Mockito test`() {
        val mock = mock<InterfaceMockito> {
            doAnswerComposable { composableStringFun() }.onComposable(this.mock) { x() }
            onComposable { y(any()) } doReturnConsecutively listOf("Hello", "There")
            onComposable { z(eq(3)) } doReturn "z"
        }
        val spy = spy(ConcreteMockito()) {
            doAnswerComposable { composableIntFun() }.onComposable(this.mock) { x(any()) }
            doReturn(45).onComposable(this.mock) { y() }
            onComposable { z(any()) } doAnswerComposable { composableIntFun2() }
            onComposable { w() } doReturn 6L
        }

        val result1 = runComposableOneShot { mock.x() }
        val result2 = runComposableOneShot { mock.y(5) }
        val result3 = runComposableOneShot { mock.y(1) }
        val result4 = runComposableOneShot { mock.z(3) }
        val result5 = runComposableOneShot { spy.x(3) }
        val result6 = runComposableOneShot { spy.y() }
        val result7 = runComposableOneShot { spy.z(6) }
        val result8 = runComposableOneShot { spy.w() }

        inOrder(mock, spy) {
            verifyComposable(mock) { x() }
            verifyComposable(mock) { y(eq(5)) }
            verifyComposable(mock) { y(eq(1)) }
            verifyComposable(mock) { z(eq(3)) }
            verifyComposable(spy) { x(eq(3)) }
            verifyComposable(spy) { y() }
            verifyComposable(spy) { z(eq(6)) }
            verifyComposable(spy) { w() }
        }
        verifyComposable(mock, times(2)) { y(any()) }
        verifyComposable(spy) { w() }
        assertEquals("Yup", result1)
        assertEquals("Hello", result2)
        assertEquals("There", result3)
        assertEquals("z", result4)
        assertEquals(42, result5)
        assertEquals(45, result6)
        assertEquals(8, result7)
        assertEquals(6L, result8)
    }
}

@Composable
fun composableIntFun(): Int = 42

@Composable
fun composableIntFun2(): Int = 8

@Composable
fun composableStringFun(): String = "Yup"

class ConcreteMockito {
    @Composable
    fun x(a: Int): Int {
        return 7
    }

    fun y(): Int {
        return 7
    }

    @Composable
    fun z(a: Int): Int {
        return 7
    }

    @Composable
    fun w(): Long = 1L
}

interface InterfaceMockito {
    @Composable
    fun x(): String

    @Composable
    fun y(a: Int): String {
        return "Yolo"
    }

    @Composable
    fun z(k: Int): String
}