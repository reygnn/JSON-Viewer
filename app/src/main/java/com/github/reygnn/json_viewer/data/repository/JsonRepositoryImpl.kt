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
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class JsonRepositoryImpl(
    private val context: Context,
    private val dispatchers: CoroutineDispatchers
) : JsonRepository {

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L

        // Local file header signature: a ZIP stream starts with "PK".
        private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
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

                val jsonString = readJsonText(uri)

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

    /**
     * Reads the picked file as UTF-8 text, size-capped and with a leading BOM
     * stripped.
     */
    private fun readJsonText(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            readCapped(input) { "Datei zu groß. Maximum: 10 MB" }
        } ?: throw IOException("Konnte Stream nicht öffnen")
        return String(bytes, Charsets.UTF_8).trimStart(Char(0xFEFF))
    }

    /**
     * Reads [input] fully into memory but refuses to exceed [MAX_FILE_SIZE_BYTES]
     * — guards sources that report no (or a wrong) size up front.
     */
    private fun readCapped(input: InputStream, message: () -> String): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_FILE_SIZE_BYTES) throw IOException(message())
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
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
