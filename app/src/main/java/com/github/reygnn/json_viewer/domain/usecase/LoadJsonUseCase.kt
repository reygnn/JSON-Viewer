package com.github.reygnn.json_viewer.domain.usecase

import android.net.Uri
import com.github.reygnn.json_viewer.domain.repository.JsonRepository

/**
 * Kapselt die Logik zum Laden einer JSON-Datei.
 * (Aktuell ein einfacher Pass-Through, aber gut für zukünftige Erweiterungen).
 */
class LoadJsonUseCase(
    private val repository: JsonRepository
) {
    suspend operator fun invoke(uri: Uri) = repository.loadAndFormatJson(uri)
}