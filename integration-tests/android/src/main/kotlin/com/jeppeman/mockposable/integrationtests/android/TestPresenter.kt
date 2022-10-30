package com.jeppeman.mockposable.integrationtests.android

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow

sealed interface TestEvent {
    object Reload : TestEvent
}

sealed interface TestModel {
    object Loading : TestModel
    class Error(val message: String) : TestModel
    class Data(val data: Int) : TestModel
}

class TestPresenter : ViewModel() {
    @Composable
    fun present(events: Flow<TestEvent>): TestModel {
        return TestModel.Data(1)
    }
}