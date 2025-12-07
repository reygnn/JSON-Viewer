package com.github.reygnn.json_viewer.domain.model

data class JsonData(
    val fileName: String,
    val fileSize: Long,
    val jsonLines: List<JsonLine>
)