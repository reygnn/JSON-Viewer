package com.github.reygnn.json_viewer.ui.viewer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.reygnn.json_viewer.R
import com.github.reygnn.json_viewer.core.parser.JsonTokenizer
import com.github.reygnn.json_viewer.domain.model.JsonLine
import com.github.reygnn.json_viewer.ui.theme.LocalJsonTokenColors
import kotlin.math.roundToLong

// "octet-stream" is included because some providers report .json that way.
private val OPEN_MIME_TYPES = arrayOf(
    "application/json",
    "application/octet-stream",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonViewerScreen(viewModel: JsonViewerViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::loadJsonFromUri)
    }

    // Show load/parse errors once, then let the ViewModel clear them.
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.jsonData?.fileName ?: stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                        )
                        state.jsonData?.let { data ->
                            Text(
                                text = formatFileSize(data.fileSize),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { openDocument.launch(OPEN_MIME_TYPES) }) {
                        Icon(
                            imageVector = Icons.Outlined.FileOpen,
                            contentDescription = stringResource(R.string.open_file),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.jsonData != null -> JsonContent(lines = state.jsonData!!.jsonLines)
                else -> Text(
                    text = stringResource(R.string.empty_view_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
        }
    }
}

@Composable
private fun JsonContent(lines: List<JsonLine>) {
    val tokenizer = remember { JsonTokenizer() }
    val tokenColors = LocalJsonTokenColors.current
    val defaultColor = MaterialTheme.colorScheme.onSurface

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = lines, key = { it.index }) { line ->
            Text(
                text = highlightJsonLine(line.content, tokenColors, defaultColor, tokenizer),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${(kb * 10).roundToLong() / 10.0} KB"
    val mb = kb / 1024.0
    return "${(mb * 10).roundToLong() / 10.0} MB"
}
