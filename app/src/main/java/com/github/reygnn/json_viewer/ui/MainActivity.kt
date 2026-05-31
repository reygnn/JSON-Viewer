package com.github.reygnn.json_viewer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.reygnn.json_viewer.JsonViewModelFactory
import com.github.reygnn.json_viewer.JsonViewerApplication
import com.github.reygnn.json_viewer.ui.theme.JsonViewerTheme
import com.github.reygnn.json_viewer.ui.viewer.JsonViewerScreen
import com.github.reygnn.json_viewer.ui.viewer.JsonViewerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            JsonViewerTheme {
                val app = application as JsonViewerApplication
                val viewModel: JsonViewerViewModel = viewModel(
                    factory = JsonViewModelFactory(app.loadJsonUseCase, app.dispatchers),
                )
                JsonViewerScreen(viewModel = viewModel)
            }
        }
    }
}
