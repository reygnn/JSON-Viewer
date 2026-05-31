package com.github.reygnn.json_viewer.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import com.github.reygnn.json_viewer.core.dispatchers.CoroutineDispatchers
import com.github.reygnn.json_viewer.core.util.Resource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboCursor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Robolectric is required here: the repository reads through a real
 * [android.content.ContentResolver] (openInputStream + OpenableColumns
 * query). Pure-JVM mocking of ContentResolver/Uri would be theatre.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JsonRepositoryImplTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val dispatchers = object : CoroutineDispatchers {
        override val main = Dispatchers.Unconfined
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
    }

    private val repository = JsonRepositoryImpl(context, dispatchers)
    private val uri: Uri = Uri.parse("content://test/sample.json")

    private fun stub(content: String, name: String = "sample.json", size: Long = content.toByteArray().size.toLong()) =
        stubRaw(content.toByteArray(), name, size)

    private fun stubRaw(bytes: ByteArray, name: String = "sample.json", size: Long = bytes.size.toLong()) {
        val resolver = context.contentResolver
        shadowOf(resolver).registerInputStream(uri, ByteArrayInputStream(bytes))

        val cursor = RoboCursor()
        cursor.setColumnNames(listOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
        cursor.setResults(arrayOf(arrayOf<Any>(name, size)))
        shadowOf(resolver).setCursor(uri, cursor)
    }

    private fun zipBytes(vararg entries: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (entryName, content) ->
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    @Test
    fun `valid json object is pretty-printed into multiple lines`() = runTest {
        stub("""{"a":1,"b":"x"}""")

        val result = repository.loadAndFormatJson(uri)

        assertThat(result).isInstanceOf(Resource.Success::class.java)
        val data = (result as Resource.Success).data
        assertThat(data.fileName).isEqualTo("sample.json")
        // toString(4) pretty-print → one line per entry plus braces
        assertThat(data.jsonLines.size).isGreaterThan(1)
        assertThat(data.jsonLines.first().content).isEqualTo("{")
    }

    @Test
    fun `valid top-level array is accepted`() = runTest {
        stub("""[1,2,3]""")

        val result = repository.loadAndFormatJson(uri)

        assertThat(result).isInstanceOf(Resource.Success::class.java)
    }

    @Test
    fun `file larger than the 10 MB guard returns an error`() = runTest {
        stub("{}", size = 11L * 1024 * 1024)

        val result = repository.loadAndFormatJson(uri)

        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun `non-json content returns an error`() = runTest {
        stub("this is not json")

        val result = repository.loadAndFormatJson(uri)

        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun `leading BOM is stripped before parsing`() = runTest {
        stub("﻿{\"a\":1}")

        val result = repository.loadAndFormatJson(uri)

        assertThat(result).isInstanceOf(Resource.Success::class.java)
    }

    // ── ZIP support (Kolibri now exports zipped JSON) ─────────────────────

    @Test
    fun `zipped json is unpacked and rendered`() = runTest {
        stubRaw(zipBytes("data.json" to """{"a":1,"b":"x"}"""), name = "export.zip")

        val result = repository.loadAndFormatJson(uri)

        assertThat(result).isInstanceOf(Resource.Success::class.java)
        val data = (result as Resource.Success).data
        assertThat(data.fileName).isEqualTo("export.zip")
        assertThat(data.jsonLines.size).isGreaterThan(1)
    }

    @Test
    fun `first json entry wins over other files in the zip`() = runTest {
        stubRaw(
            zipBytes(
                "readme.txt" to "not json at all",
                "payload.json" to """{"ok":true}""",
            ),
            name = "export.zip",
        )

        // A Success proves the .json entry was chosen — the .txt would not parse.
        val result = repository.loadAndFormatJson(uri)
        assertThat(result).isInstanceOf(Resource.Success::class.java)
    }

    @Test
    fun `zip without a json-named entry falls back to the first entry`() = runTest {
        stubRaw(zipBytes("payload" to """{"a":1}"""), name = "export.zip")

        val result = repository.loadAndFormatJson(uri)
        assertThat(result).isInstanceOf(Resource.Success::class.java)
    }

    @Test
    fun `zip whose only entry is not json returns an error`() = runTest {
        stubRaw(zipBytes("notes.txt" to "just some text"), name = "export.zip")

        val result = repository.loadAndFormatJson(uri)
        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun `corrupted zip returns an error`() = runTest {
        // Valid ZIP magic, garbage payload → ZipInputStream fails to read it.
        stubRaw(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 1, 2, 3, 4, 5, 6, 7, 8), name = "broken.zip")

        val result = repository.loadAndFormatJson(uri)
        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun `unpacked json exceeding the 10 MB cap returns an error`() = runTest {
        // Highly compressible: tiny compressed (passes the source guard) but
        // > 10 MB unpacked (trips the zip-bomb cap in readCapped).
        val huge = "a".repeat(11 * 1024 * 1024)
        stubRaw(zipBytes("big.json" to huge), name = "bomb.zip")

        val result = repository.loadAndFormatJson(uri)
        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }
}
