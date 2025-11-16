package com.github.reygnn.json_viewer.ui.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.reygnn.json_viewer.core.dispatchers.CoroutineDispatchers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Eine vereinfachte, crash-sichere BaseViewModel,
 * inspiriert von deinem Kolibri-Beispiel.
 */
abstract class BaseViewModel(
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { context, throwable ->
        Timber.e(throwable, "Unbehandelte Exception im ViewModel: $context")
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
                Timber.e(t, "Fehler in launchSafe-Block")
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
        Timber.e(throwable, "BaseViewModel handleError")
    }
}