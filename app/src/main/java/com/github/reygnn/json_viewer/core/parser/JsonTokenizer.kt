package com.github.reygnn.json_viewer.core.parser

import com.github.reygnn.json_viewer.domain.model.JsonToken
import com.github.reygnn.json_viewer.domain.model.TokenType

class JsonTokenizer {

    fun tokenize(line: String): List<JsonToken> {
        val tokens = mutableListOf<JsonToken>()
        var i = 0

        while (i < line.length) {
            when {
                line[i].isWhitespace() -> i++

                line[i] == '"' -> {
                    val start = i
                    i++
                    while (i < line.length) {
                        if (line[i] == '"' && line[i - 1] != '\\') break
                        i++
                    }
                    i++

                    val lookAhead = line.drop(i).trimStart()
                    val type = if (lookAhead.startsWith(':')) TokenType.KEY else TokenType.STRING

                    tokens.add(JsonToken(type, start, i))
                }

                line[i] in "{}[],:" -> {
                        tokens.add(JsonToken(TokenType.PUNCTUATION, i, i + 1))
                        i++
            }

            line.regionMatches(i, "true", 0, 4, ignoreCase = false) -> {
                tokens.add(JsonToken(TokenType.BOOLEAN, i, i + 4))
                i += 4
            }

            line.regionMatches(i, "false", 0, 5, ignoreCase = false) -> {
                tokens.add(JsonToken(TokenType.BOOLEAN, i, i + 5))
                i += 5
            }

            line.regionMatches(i, "null", 0, 4, ignoreCase = false) -> {
                tokens.add(JsonToken(TokenType.NULL, i, i + 4))
                i += 4
            }

            line[i].isDigit() || line[i] == '-' -> {
                val start = i
                while (i < line.length && (line[i].isDigit() || line[i] in ".eE+-")) {
                    i++
                }
                tokens.add(JsonToken(TokenType.NUMBER, start, i))
            }

            else -> i++
        }
    }
    return tokens
}
}