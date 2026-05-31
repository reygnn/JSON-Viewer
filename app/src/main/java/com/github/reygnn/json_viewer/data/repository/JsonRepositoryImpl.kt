package com.github.reygnn.json_viewer.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.github.reygnn.json_viewer.core.dispatchers.CoroutineDispatchers
import com.github.reygnn.json_viewer.core.util.Resource
import com.github.reygnn.json_viewer.domain.model.JsonData
import com.github.reygnn.json_viewer.domain.model.JsonLine
import com.github.reygnn.json_viewer.domain.repository.JsonRepository
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException

class JsonRepositoryImpl(
    private val context: Context,
    private val dispatchers: CoroutineDispatchers
) : JsonRepository {

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L
    }

    override suspend fun loadAndFormatJson(uri: Uri): Resource<JsonData> {
        return withContext(dispatchers.io) {
            try {
                val metaData = getFileMetaData(uri)

                if (metaData.second > MAX_FILE_SIZE_BYTES) {
                    return@withContext Resource.Error(
                        "Datei zu groß (${metaData.second / 1024 / 1024} MB). Maximum: 10 MB"
                    )
                }

                val jsonString = readTextFromUri(uri)

                val tokener = JSONTokener(jsonString)
                val value = tokener.nextValue()

                val formattedJsonString = when (value) {
                    is JSONObject -> value.toString(4)
                    is JSONArray -> value.toString(4)
                    else -> throw IllegalArgumentException("Kein gültiges JSON-Objekt oder -Array")
                }

                val jsonLinesList = formattedJsonString.lines()
                    .mapIndexed { index, content -> JsonLine(index, content) }

                Resource.Success(
                    JsonData(
                        fileName = metaData.first,
                        fileSize = metaData.second,
                        jsonLines = jsonLinesList
                    )
                )

            } catch (e: Exception) {
                Resource.Error("Fehler beim Laden/Parsen: ${e.localizedMessage}")
            }
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use {
            it.bufferedReader().readText().trimStart('\uFEFF')
        } ?: throw IOException("Konnte Stream nicht öffnen")
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