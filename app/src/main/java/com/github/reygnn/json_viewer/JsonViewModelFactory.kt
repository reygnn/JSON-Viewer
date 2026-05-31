package com.github.reygnn.json_viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.reygnn.json_viewer.core.dispatchers.CoroutineDispatchers
import com.github.reygnn.json_viewer.domain.usecase.LoadJsonUseCase
import com.github.reygnn.json_viewer.ui.viewer.JsonViewerViewModel

/**
 * Manual ViewModel factory (replaces Hilt's @HiltViewModel). Hands each
 * ViewModel its dependencies from the [JsonViewerApplication] container.
 */
class JsonViewModelFactory(
    private val loadJsonUseCase: LoadJsonUseCase,
    private val dispatchers: CoroutineDispatchers,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        JsonViewerViewModel::class.java -> JsonViewerViewModel(loadJsonUseCase, dispatchers) as T
        else -> error("Unknown ViewModel: ${modelClass.name}")
    }
}
