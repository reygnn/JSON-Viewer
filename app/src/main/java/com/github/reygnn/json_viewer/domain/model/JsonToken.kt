package com.github.reygnn.json_viewer.domain.model

data class JsonToken(
    val type: TokenType,
    val start: Int,
    val end: Int
)

enum class TokenType {
    KEY,
    STRING,
    NUMBER,
    BOOLEAN,
    NULL,
    PUNCTUATION
}