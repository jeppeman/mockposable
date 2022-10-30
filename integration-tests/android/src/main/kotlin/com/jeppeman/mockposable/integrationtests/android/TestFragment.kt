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
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class TestFragment : Fragment() {
    private val events = MutableSharedFlow<TestEvent>(replay = 1)
    lateinit var present: @Composable (MutableSharedFlow<TestEvent>) -> TestModel

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
        val models = lifecycleScope.launchMolecule(RecompositionClock.Immediate) {
            present(events)
        }

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