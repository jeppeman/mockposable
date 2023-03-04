package com.jeppeman.mockposable

import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

object Mockposable {
    @Volatile
    var activeComposer: Composer? = null
}

/**
 * Runs the initial composition of a composable function and returns its result.
 */

@Suppress("UNCHECKED_CAST")
fun <T> runComposableOneShot(useActiveComposer: Boolean = false, block: @Composable () -> T): T {
    if (!useActiveComposer || Mockposable.activeComposer == null) {
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
    } else {
        return block::class.java.methods
            .first { it.name == "invoke" }
            .apply { isAccessible = true }
            .invoke(block, Mockposable.activeComposer, -1) as T
    }
}

private object UnitApplier : AbstractApplier<Any>(Unit) {
    override fun onClear() = Unit
    override fun insertBottomUp(index: Int, instance: Any) = Unit
    override fun insertTopDown(index: Int, instance: Any) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun remove(index: Int, count: Int) = Unit
}