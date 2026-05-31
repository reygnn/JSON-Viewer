package com.github.reygnn.json_viewer.ui.viewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.github.reygnn.json_viewer.core.parser.JsonTokenizer
import com.github.reygnn.json_viewer.ui.theme.JsonTokenColors
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure-JVM tests for the tokenizer → AnnotatedString mapping. AnnotatedString,
 * SpanStyle and Color are plain Kotlin (no Android runtime), so no Robolectric.
 */
class JsonHighlighterTest {

    private val tokenizer = JsonTokenizer()

    private val colors = JsonTokenColors(
        key = Color(0xFF111111),
        string = Color(0xFF222222),
        number = Color(0xFF333333),
        boolean = Color(0xFF444444),
        nul = Color(0xFF555555),
        punctuation = Color(0xFF666666),
    )
    private val default = Color(0xFF000000)

    private fun highlight(line: String): AnnotatedString =
        highlightJsonLine(line, colors, default, tokenizer)

    // Last matching span wins (token styles are added after the base style).
    private fun AnnotatedString.colorAt(index: Int): Color? =
        spanStyles.lastOrNull { index in it.start until it.end }?.item?.color

    @Test
    fun `key, punctuation and string get their token colours`() {
        val line = """  "name": "John""""
        val out = highlight(line)

        assertThat(out.text).isEqualTo(line)
        assertThat(out.colorAt(3)).isEqualTo(colors.key)          // inside "name"
        assertThat(out.colorAt(8)).isEqualTo(colors.punctuation)  // the colon
        assertThat(out.colorAt(12)).isEqualTo(colors.string)      // inside "John"
    }

    @Test
    fun `whitespace keeps the default colour`() {
        val out = highlight("""  "name": "John""""")
        assertThat(out.colorAt(0)).isEqualTo(default)   // leading space
        assertThat(out.colorAt(9)).isEqualTo(default)   // space after colon
    }

    @Test
    fun `number value uses the number colour`() {
        val line = """  "age": 30"""
        val out = highlight(line)
        // "30" are the last two characters
        assertThat(out.colorAt(line.length - 1)).isEqualTo(colors.number)
    }

    @Test
    fun `boolean and null get their colours`() {
        assertThat(highlight("""  "a": true""").colorAt("""  "a": tru""".length))
            .isEqualTo(colors.boolean)
        assertThat(highlight("""  "a": null""").colorAt("""  "a": nul""".length))
            .isEqualTo(colors.nul)
    }

    @Test
    fun `unterminated string does not crash and preserves the text`() {
        val line = """  "x": "abc"""  // no closing quote → tokenizer end runs past line
        val out = highlight(line)
        assertThat(out.text).isEqualTo(line)
    }

    @Test
    fun `blank line has no token spans`() {
        val out = highlight("    ")
        // only the base style may be present; no token colour anywhere
        assertThat(out.colorAt(1)).isEqualTo(default)
    }
}
