package com.github.reygnn.json_viewer.domain.repository

import android.net.Uri
import com.github.reygnn.json_viewer.core.util.Resource
import com.github.reygnn.json_viewer.domain.model.JsonData

interface JsonRepository {
    /**
     * Lädt, parst und formatiert eine JSON-Datei von einer gegebenen content:// URI.
     */
    suspend fun loadAndFormatJson(uri: Uri): Resource<JsonData>
}