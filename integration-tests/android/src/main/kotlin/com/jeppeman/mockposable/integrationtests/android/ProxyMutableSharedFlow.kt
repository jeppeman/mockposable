package com.jeppeman.mockposable.integrationtests.android

import kotlinx.coroutines.flow.MutableSharedFlow

open class ProxyMutableSharedFlow<T>(
    private val realFlow: MutableSharedFlow<T> = MutableSharedFlow(replay = 1)
) : MutableSharedFlow<T> by realFlow
