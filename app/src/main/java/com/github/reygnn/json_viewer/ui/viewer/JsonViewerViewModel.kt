package com.github.reygnn.json_viewer.ui.viewer

import android.net.Uri
import com.github.reygnn.json_viewer.core.dispatchers.CoroutineDispatchers
import com.github.reygnn.json_viewer.core.util.Resource
import com.github.reygnn.json_viewer.domain.model.JsonData
import com.github.reygnn.json_viewer.domain.usecase.LoadJsonUseCase
import com.github.reygnn.json_viewer.ui.util.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Der State hält jetzt JsonData, welches die Liste der Zeilen beinhaltet
data class JsonViewerUiState(
    val isLoading: Boolean = false,
    val jsonData: JsonData? = null,
    val errorMessage: String? = null
)

class JsonViewerViewModel(
    private val loadJsonUseCase: LoadJsonUseCase,
    dispatchers: CoroutineDispatchers
) : BaseViewModel(dispatchers) {

    private val _uiState = MutableStateFlow(JsonViewerUiState())
    val uiState: StateFlow<JsonViewerUiState> = _uiState.asStateFlow()

    fun loadJsonFromUri(uri: Uri) {
        // launchSafe kommt aus deinem BaseViewModel und fängt Crashes ab
        launchSafe {
            // 1. Ladezustand setzen
            _uiState.update { it.copy(isLoading = true, errorMessage = null, jsonData = null) }

            // 2. UseCase aufrufen (Dieser liest File -> Formatiert JSON -> Splittet in Liste)
            when (val result = loadJsonUseCase(uri)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            jsonData = result.data, // Hier ist jetzt die List<String> drin
                            errorMessage = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            jsonData = null,
                            errorMessage = result.message ?: "Unbekannter Fehler beim Laden"
                        )
                    }
                }
                is Resource.Loading -> {
                    // Optional: Falls der UseCase Zwischenstände sendet
                }
            }
        }
    }

    /**
     * Wird aufgerufen, wenn im launchSafe Block eine Exception fliegt,
     * die nicht durch Resource.Error abgefangen wurde.
     */
    override fun handleError(throwable: Throwable) {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = "Kritischer Fehler: ${throwable.localizedMessage}"
            )
        }
    }

    // Optional: Methode um Error-Message zurückzusetzen, nachdem Snackbar angezeigt wurde
    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}