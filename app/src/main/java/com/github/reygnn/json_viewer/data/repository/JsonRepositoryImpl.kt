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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

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
     * Reads the JSON text from the picked file. A plain `.json` is read
     * directly; a ZIP (Kolibri now exports zipped JSON) is unpacked and its
     * JSON entry returned. Detection is by content (the ZIP magic bytes), not
     * by file name / MIME, since SAF content URIs rarely carry a reliable one.
     */
    private fun readJsonText(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            readCapped(input) { "Datei zu groß. Maximum: 10 MB" }
        } ?: throw IOException("Konnte Stream nicht öffnen")

        val text = if (looksLikeZip(bytes)) {
            extractJsonFromZip(bytes)
        } else {
            String(bytes, Charsets.UTF_8)
        }
        return text.trimStart(Char(0xFEFF))
    }

    private fun looksLikeZip(bytes: ByteArray): Boolean =
        bytes.size >= ZIP_MAGIC.size && ZIP_MAGIC.indices.all { bytes[it] == ZIP_MAGIC[it] }

    /**
     * Picks the JSON out of a ZIP: the first entry whose name ends in `.json`
     * (case-insensitive), or — if none is named that way — the first file
     * entry. Each entry is read through [readCapped] so a zip bomb can't blow
     * past the size limit.
     */
    private fun extractJsonFromZip(bytes: ByteArray): String {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var fallback: ByteArray? = null
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name.substringAfterLast('/')
                    if (name.endsWith(".json", ignoreCase = true)) {
                        return String(readCapped(zip) { "Entpackte JSON zu groß. Maximum: 10 MB" }, Charsets.UTF_8)
                    }
                    if (fallback == null) {
                        fallback = readCapped(zip) { "Entpackter Eintrag zu groß. Maximum: 10 MB" }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            return fallback?.let { String(it, Charsets.UTF_8) }
                ?: throw IllegalArgumentException("Keine Datei im ZIP-Archiv gefunden")
        }
    }

    /**
     * Reads [input] fully into memory but refuses to exceed [MAX_FILE_SIZE_BYTES]
     * — guards both unknown-size sources and zip-bomb decompression.
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
