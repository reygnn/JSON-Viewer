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

    private fun stub(content: String, name: String = "sample.json", size: Long = content.toByteArray().size.toLong()) {
        val resolver = context.contentResolver
        shadowOf(resolver).registerInputStream(uri, ByteArrayInputStream(content.toByteArray()))

        val cursor = RoboCursor()
        cursor.setColumnNames(listOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
        cursor.setResults(arrayOf(arrayOf<Any>(name, size)))
        shadowOf(resolver).setCursor(uri, cursor)
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
}
