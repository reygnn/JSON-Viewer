package com.github.reygnn.json_viewer.ui.util

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.reygnn.json_viewer.core.dispatchers.CoroutineDispatchers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Eine vereinfachte, crash-sichere BaseViewModel,
 * inspiriert von deinem Kolibri-Beispiel.
 */
abstract class BaseViewModel(
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { context, throwable ->
        Log.e(TAG, "Unbehandelte Exception im ViewModel: $context", throwable)
        handleError(throwable)
    }

    /**
     * Startet eine Coroutine im Main-Kontext mit sicherem Error-Handling.
     */
    protected fun launchSafe(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(dispatchers.main + coroutineExceptionHandler) {
            try {
                block()
            } catch (t: Throwable) {
                Log.e(TAG, "Fehler in launchSafe-Block", t)
                handleError(t)
            }
        }
    }

    /**
     * Kann von untergeordneten ViewModels überschrieben werden,
     * um Fehler spezifisch im UI-State zu setzen.
     */
    protected open fun handleError(throwable: Throwable) {
        // Standard-Implementierung loggt nur
        Log.e(TAG, "BaseViewModel handleError", throwable)
    }

    private companion object {
        const val TAG = "BaseViewModel"
    }
}