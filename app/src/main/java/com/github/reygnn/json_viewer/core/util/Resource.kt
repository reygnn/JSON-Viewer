package com.github.reygnn.json_viewer.core.util

/**
 * Ein generischer Wrapper für Daten, der Lade-, Erfolgs- und Fehlerzustände kapselt.
 * Inspiriert von deiner crash-sicheren Architektur.
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}