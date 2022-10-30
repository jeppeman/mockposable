package com.jeppeman.mockposable.integrationtests.jvm

import androidx.compose.runtime.Composable
import com.jeppeman.mockposable.mockk.*
import com.jeppeman.mockposable.runComposableOneShot
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.assertEquals
import org.junit.Test

class JvmMockKTest {
    @Test
    fun `composable MockK test`() {
        val mockk = mockk<InterfaceMockK> {
            everyComposable { x() } answersComposable { composableStringAnswer() } andThenComposable { composableStringAnswer2() }
            everyComposable { y(any()) } returns "y"
            everyComposable { z(any()) } returns "z"
        }
        val spyk = spyk(ConcreteMockK()) {
            everyComposable { x(any()) } answersComposable { composableIntAnswer() }
            everyComposable { y } answersComposable  { composableIntAnswer2() }
            everyComposable { z() } returns "44"
        }

        val result1 = runComposableOneShot { mockk.x() }
        val result2 = runComposableOneShot { mockk.x() }
        val result3 = runComposableOneShot { mockk.y(2) }
        val result4 = runComposableOneShot { mockk.z(3) }
        val result5 = runComposableOneShot { spyk.x(5) }
        val result6 = runComposableOneShot { spyk.y }
        val result7 = runComposableOneShot { spyk.z() }

        verifyComposableAll {
            spyk.y
            mockk.x()
            mockk.x()
            spyk.x(5)
            mockk.z(3)
            spyk.z()
            mockk.y(2)
        }
        verifyComposableOrder {
            mockk.x()
            mockk.y(2)
            mockk.z(3)
            spyk.x(5)
            spyk.y
            spyk.z()
        }
        verifyComposableSequence {
            mockk.x()
            mockk.x()
            mockk.y(2)
            mockk.z(3)
            spyk.x(5)
            spyk.y
            spyk.z()
        }
        verifyComposable(exactly = 2) { mockk.x() }
        assertEquals("Hello", result1)
        assertEquals("There", result2)
        assertEquals("y", result3)
        assertEquals("z", result4)
        assertEquals(42, result5)
        assertEquals(43, result6)
        assertEquals("44", result7)
    }
}

@Composable
fun composableIntAnswer(): Int = 42

@Composable
fun composableIntAnswer2(): Int = 43

@Composable
fun composableStringAnswer(): String = "Hello"

@Composable
fun composableStringAnswer2(): String = "There"

class ConcreteMockK {
    @Composable
    fun x(a: Int): Int {
        return 7
    }

    val y: Int
        @Composable get() = 7

    @Composable
    fun z(): String = "z"
}

interface InterfaceMockK {
    @Composable
    fun x(): String

    @Composable
    fun y(a: Int): String {
        return "y$a"
    }

    @Composable
    fun z(k: Int): String
}