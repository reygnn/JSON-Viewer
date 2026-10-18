package com.github.reygnn.json_viewer.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.github.reygnn.json_viewer.JsonViewModelFactory
import com.github.reygnn.json_viewer.JsonViewerApplication
import com.github.reygnn.json_viewer.ui.theme.JsonViewerTheme
import com.github.reygnn.json_viewer.ui.viewer.JsonViewerScreen
import com.github.reygnn.json_viewer.ui.viewer.JsonViewerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: JsonViewerViewModel by viewModels {
        val app = application as JsonViewerApplication
        JsonViewModelFactory(app.loadJsonUseCase, app.dispatchers)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Only on a fresh start: after a config change the ViewModel already
        // holds the file, re-loading it would just flash the spinner.
        if (savedInstanceState == null) openFromIntent(intent)
        setContent {
            JsonViewerTheme {
                JsonViewerScreen(viewModel = viewModel)
            }
        }
    }

    // singleTop: a second "Open with" while the viewer is on top lands here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openFromIntent(intent)
    }

    private fun openFromIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let(viewModel::loadJsonFromUri)
        }
    }
}
