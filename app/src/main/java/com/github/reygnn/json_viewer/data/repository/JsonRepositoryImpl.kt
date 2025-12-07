package com.github.reygnn.json_viewer.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.github.reygnn.json_viewer.core.dispatchers.CoroutineDispatchers
import com.github.reygnn.json_viewer.core.util.Resource
import com.github.reygnn.json_viewer.domain.model.JsonData
import com.github.reygnn.json_viewer.domain.model.JsonLine
import com.github.reygnn.json_viewer.domain.repository.JsonRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class JsonRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatchers
) : JsonRepository {

    override suspend fun loadAndFormatJson(uri: Uri): Resource<JsonData> {
        return withContext(dispatchers.io) {
            try {
                // 1. Datei-Metadaten lesen (Name & Größe)
                val metaData = getFileMetaData(uri)

                // 2. Inhalt lesen
                val jsonString = readTextFromUri(uri)

                // 3. Parsen und Formatieren (4 Spaces Einrückung)
                // Wir nutzen JSONTokener, um zu erkennen, ob es ein Objekt {} oder Array [] ist
                val tokener = JSONTokener(jsonString)
                val value = tokener.nextValue()

                val formattedJsonString = when (value) {
                    is JSONObject -> value.toString(4)
                    is JSONArray -> value.toString(4)
                    else -> throw IllegalArgumentException("Datei enthält kein gültiges JSON-Objekt oder -Array")
                }

                // 4. WICHTIG: String in eine Liste von Zeilen umwandeln für den RecyclerView!
                val jsonLinesList = formattedJsonString.lines()
                    .mapIndexed { index, content -> JsonLine(index, content) }

                // 5. JsonData erstellen
                Resource.Success(
                    JsonData(
                        fileName = metaData.first,
                        fileSize = metaData.second,
                        jsonLines = jsonLinesList // <-- Hier übergeben wir die Liste
                    )
                )

            } catch (e: Exception) {
                e.printStackTrace()
                Resource.Error("Fehler beim Laden/Parsen: ${e.localizedMessage}")
            }
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }

    private fun getFileMetaData(uri: Uri): Pair<String, Long> {
        var name = "Unknown.json"
        var size = 0L

        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

                if (nameIndex != -1) name = it.getString(nameIndex)
                if (sizeIndex != -1) size = it.getLong(sizeIndex)
            }
        }
        return Pair(name, size)
    }
}