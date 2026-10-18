package com.github.reygnn.json_viewer.data.repository

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File

/**
 * Stands in for a SAF document provider: answers the [OpenableColumns]
 * query with [name] / [size] and serves [file] as the content. Registered
 * through `Robolectric.setupContentProvider`, so the repository goes through
 * the real ContentResolver → ContentProvider path.
 *
 * [size] is set independently of the file so a test can simulate a provider
 * that reports a wrong size.
 */
class FakeDocumentProvider : ContentProvider() {

    lateinit var file: File
    var name: String = "sample.json"
    var size: Long = 0

    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
        .apply { addRow(arrayOf<Any>(name, size)) }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

    override fun getType(uri: Uri) = "application/json"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
}
