package com.jeppeman.mockposable.integrationtests.android

import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableSharedFlow

@Composable
fun ComposeTestView(
    presenter: TestPresenter = viewModel(),
    events: MutableSharedFlow<TestEvent> = remember { MutableSharedFlow(replay = 1) }
) = when (val model = presenter.present(events)) {
    TestModel.Loading -> CircularProgressIndicator(modifier = Modifier.testTag("progress"))
    is TestModel.Error -> {
        Text(text = model.message)
        Button(onClick = { events.tryEmit(TestEvent.Reload) }) {
            Text(text = "Try again")
        }
    }
    is TestModel.Data -> Text(text = model.data.toString())
}
