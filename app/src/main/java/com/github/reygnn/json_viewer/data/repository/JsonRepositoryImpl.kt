package com.github.reygnn.json_viewer.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.github.reygnn.json_viewer.core.dispatchers.CoroutineDispatchers
import com.github.reygnn.json_viewer.core.util.Resource
import com.github.reygnn.json_viewer.domain.model.JsonData
import com.github.reygnn.json_viewer.domain.repository.JsonRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class JsonRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatchers,
    private val jsonParser: Json // Von AppModule bereitgestellt
) : JsonRepository {

    private data class FileDetails(val name: String, val size: Long)

    override suspend fun loadAndFormatJson(uri: Uri): Resource<JsonData> = withContext(dispatchers.io) {
        // Deine "crash-sichere" Philosophie: Alles in try-catch(Throwable)
        try {
            val contentResolver = context.contentResolver

            // 1. Datei-Details (Name, Größe) sicher abrufen
            val details = getFileDetails(uri)
                ?: return@withContext Resource.Error("Konnte Dateidetails nicht laden.")

            // 2. Datei-Inhalt sicher lesen
            val rawJsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext Resource.Error("Konnte Datei nicht öffnen.")

            if (rawJsonString.isBlank()) {
                return@withContext Resource.Error("Datei ist leer.")
            }

            // 3. JSON parsen und neu formatieren (Pretty Print)
            val jsonElement = jsonParser.decodeFromString<JsonElement>(rawJsonString)
            val formattedJson = jsonParser.encodeToString(JsonElement.serializer(), jsonElement)

            val jsonData = JsonData(
                fileName = details.name,
                formattedJson = formattedJson,
                fileSize = details.size
            )
            Resource.Success(jsonData)

        } catch (e: Throwable) {
            // Fängt alles ab (SerializationException, IOException, SecurityException, OutOfMemoryError, etc.)
            Timber.e(e, "Fehler beim Laden/Parsen von JSON: $uri")
            Resource.Error("Fehler beim Parsen: ${e.javaClass.simpleName} - ${e.message}")
        }
    }

    private fun getFileDetails(uri: Uri): FileDetails? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                    val name = if (nameIndex != -1) cursor.getString(nameIndex) else "unbekannt.json"
                    val size = if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L

                    FileDetails(name, size)
                } else {
                    null
                }
            }
        } catch (e: Throwable) {
            Timber.w(e, "Konnte Dateidetails nicht abrufen")
            // Fallback: Dateiname aus URI extrahieren (weniger zuverlässig)
            val name = uri.lastPathSegment ?: "unbekannt.json"
            FileDetails(name, 0L)
        }
    }
}