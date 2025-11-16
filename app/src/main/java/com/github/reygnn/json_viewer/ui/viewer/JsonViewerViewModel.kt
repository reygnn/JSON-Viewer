package com.github.reygnn.json_viewer.ui.viewer

import android.net.Uri
import com.github.reygnn.json_viewer.core.dispatchers.CoroutineDispatchers
import com.github.reygnn.json_viewer.core.util.Resource
import com.github.reygnn.json_viewer.domain.model.JsonData
import com.github.reygnn.json_viewer.domain.usecase.LoadJsonUseCase
import com.github.reygnn.json_viewer.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// UI-State für das Fragment
data class JsonViewerUiState(
    val isLoading: Boolean = false,
    val jsonData: JsonData? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class JsonViewerViewModel @Inject constructor(
    private val loadJsonUseCase: LoadJsonUseCase,
    dispatchers: CoroutineDispatchers
) : BaseViewModel(dispatchers) {

    private val _uiState = MutableStateFlow(JsonViewerUiState())
    val uiState: StateFlow<JsonViewerUiState> = _uiState.asStateFlow()

    fun loadJsonFromUri(uri: Uri) {
        launchSafe {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = loadJsonUseCase(uri)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, jsonData = result.data)
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, jsonData = null, errorMessage = result.message)
                    }
                }
                is Resource.Loading -> {
                    // Wird hier nicht verwendet, da wir den Ladezustand manuell setzen
                }
            }
        }
    }

    /**
     * Überschreibt die handleError-Methode der BaseViewModel,
     * um Fehler im UI-State zu setzen.
     */
    override fun handleError(throwable: Throwable) {
        _uiState.update {
            it.copy(
                isLoading = false,
                jsonData = null,
                errorMessage = "Kritischer Fehler: ${throwable.message}"
            )
        }
    }
}