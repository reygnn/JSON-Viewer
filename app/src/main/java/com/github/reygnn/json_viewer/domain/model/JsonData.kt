package com.github.reygnn.json_viewer.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class JsonData(
    val fileName: String,
    val formattedJson: String,
    val fileSize: Long
) : Parcelable