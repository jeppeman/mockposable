package com.jeppeman.mockposable.integrationtests.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class TestFragment : Fragment() {
    lateinit var events: MutableSharedFlow<TestEvent>
    lateinit var composableLauncher: CoroutineScope.(@Composable () -> TestModel) -> Flow<TestModel>
    lateinit var presenter: TestPresenter

    private val content: LinearLayout by lazy {
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val models = lifecycleScope.composableLauncher { presenter.present(events) }
        lifecycleScope.launch { models.collect(::render) }
    }

    private fun render(model: TestModel) = content.run {
        removeAllViews()
        when (model) {
            TestModel.Loading -> content.apply {
                addView(ProgressBar(context).apply { tag = "progress" })
            }
            is TestModel.Error -> {
                addView(TextView(context).apply { text = model.message })
                addView(Button(context).apply {
                    text = "Try again"
                    setOnClickListener { events.tryEmit(TestEvent.Reload) }
                })
            }
            is TestModel.Data -> {
                addView(TextView(context).apply { text = model.data.toString() })
            }
        }
    }
}