package com.github.reygnn.json_viewer.core.parser

import com.github.reygnn.json_viewer.domain.model.TokenType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure JVM tests for the per-line lexer. No Android runtime, no mocks —
 * feed a formatted line, assert the token types (and a few spans).
 */
class JsonTokenizerTest {

    private val tokenizer = JsonTokenizer()

    private fun types(line: String) = tokenizer.tokenize(line).map { it.type }

    @Test
    fun `string value before colon is a KEY, after colon is a STRING`() {
        assertThat(types("""  "name": "John",""")).containsExactly(
            TokenType.KEY,
            TokenType.PUNCTUATION,   // :
            TokenType.STRING,
            TokenType.PUNCTUATION,   // ,
        ).inOrder()
    }

    @Test
    fun `number value is tokenized as NUMBER`() {
        assertThat(types("""  "age": 30,""")).containsExactly(
            TokenType.KEY,
            TokenType.PUNCTUATION,
            TokenType.NUMBER,
            TokenType.PUNCTUATION,
        ).inOrder()
    }

    @Test
    fun `negative and exponent numbers stay a single NUMBER token`() {
        assertThat(types("""  "v": -1.5e10""")).containsExactly(
            TokenType.KEY,
            TokenType.PUNCTUATION,
            TokenType.NUMBER,
        ).inOrder()
    }

    @Test
    fun `boolean and null literals get their own types`() {
        assertThat(types("""  "active": true""").last()).isEqualTo(TokenType.BOOLEAN)
        assertThat(types("""  "missing": null""").last()).isEqualTo(TokenType.NULL)
        assertThat(types("""  "off": false""").last()).isEqualTo(TokenType.BOOLEAN)
    }

    @Test
    fun `structural braces are punctuation`() {
        assertThat(types("{")).containsExactly(TokenType.PUNCTUATION)
        assertThat(types("  }")).containsExactly(TokenType.PUNCTUATION)
        assertThat(types("  ],")).containsExactly(TokenType.PUNCTUATION, TokenType.PUNCTUATION)
    }

    @Test
    fun `token spans map back to the source substring`() {
        val line = """  "name": "John""""
        val tokens = tokenizer.tokenize(line)
        val key = tokens.first { it.type == TokenType.KEY }
        val string = tokens.first { it.type == TokenType.STRING }
        assertThat(line.substring(key.start, key.end)).isEqualTo("\"name\"")
        assertThat(line.substring(string.start, string.end)).isEqualTo("\"John\"")
    }

    @Test
    fun `escaped quote inside a string does not terminate it early`() {
        // Line content: ["path": "a\"b"] — the inner \" must not end the value string.
        val line = "  \"path\": \"a\\\"b\""
        val string = tokenizer.tokenize(line).first { it.type == TokenType.STRING }
        assertThat(line.substring(string.start, string.end)).isEqualTo("\"a\\\"b\"")
    }

    @Test
    fun `blank line yields no tokens`() {
        assertThat(tokenizer.tokenize("    ")).isEmpty()
    }
}
