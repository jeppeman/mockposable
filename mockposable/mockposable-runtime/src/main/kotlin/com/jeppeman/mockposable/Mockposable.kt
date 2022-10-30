package com.jeppeman.mockposable

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

/**
 * Runs the initial composition of a composable function and returns its result.
 */
@Suppress("UNCHECKED_CAST")
fun <T> runComposableOneShot(block: @Composable () -> T): T {
    var result: T = null as T
    try {
        runBlocking {
            val recomposer = Recomposer(coroutineContext)
            val composition = Composition(UnitApplier, recomposer)
            composition.setContent {
                result = block()
                composition.dispose()
                recomposer.close()
                /* We're just looking to execute block() and escape as soon as possible. Perhaps
                there is a more elegant way of doing this, but this gets the job done. */
                cancel()
            }
        }
    } catch (e: CancellationException) {
        // We're expecting this
    }

    return result
}

private object UnitApplier : AbstractApplier<Unit>(Unit) {
    override fun onClear() = Unit
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun remove(index: Int, count: Int) = Unit
}